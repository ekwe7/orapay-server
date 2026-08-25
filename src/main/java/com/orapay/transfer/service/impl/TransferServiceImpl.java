package com.orapay.transfer.service.impl;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.common.exception.InsufficientFundsException;
import com.orapay.transfer.dto.request.TransferRequestDto;
import com.orapay.transfer.dto.response.TransferResponseDto;
import com.orapay.transfer.event.TransferCompletedEvent;
import com.orapay.transfer.event.TransferFailedEvent;
import com.orapay.transfer.event.TransferInitiatedEvent;
import com.orapay.transfer.mapper.TransferMapper;
import com.orapay.transfer.model.Transaction;
import com.orapay.transfer.model.TransferStatus;
import com.orapay.transfer.repository.TransactionRepository;
import com.orapay.transfer.service.TransferService;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import com.orapay.wallet.service.impl.WalletLockManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TransferServiceImpl implements TransferService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletLockManager walletLockManager;
    private final TransferMapper transferMapper;
    private final EventPublisher eventPublisher;

    private final Counter transferRequestsCounter;
    private final Counter transferSuccessCounter;
    private final Counter transferFailedCounter;
    private final Timer transferExecutionTimer;
    private final MeterRegistry meterRegistry;

    public TransferServiceImpl(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            WalletLockManager walletLockManager,
            TransferMapper transferMapper,
            EventPublisher eventPublisher,
            MeterRegistry meterRegistry
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletLockManager = walletLockManager;
        this.transferMapper = transferMapper;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;

        this.transferRequestsCounter = Counter.builder("transfer.requests.total")
                .description("Total number of P2P transfer requests")
                .register(meterRegistry);

        this.transferSuccessCounter = Counter.builder("transfer.success.total")
                .description("Total number of successful P2P transfers")
                .register(meterRegistry);

        this.transferFailedCounter = Counter.builder("transfer.failed.total")
                .description("Total number of failed P2P transfers")
                .register(meterRegistry);

        this.transferExecutionTimer = Timer.builder("transfer.execution.duration")
                .description("Duration spent processing P2P transfers")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public TransferResponseDto processTransfer(TransferRequestDto requestDto) {
        Timer.Sample sample = Timer.start(meterRegistry);
        transferRequestsCounter.increment();

        Transaction pendingTransaction = null;
        try {
            // 1. Resolve recipient wallet by identifier (UUID, phone, or account number)
            Wallet recipientWalletRef = walletRepository.resolveRecipientByIdentifier(requestDto.getRecipientIdentifier())
                    .orElseThrow(() -> new BusinessRuleException("Recipient wallet not found for identifier: " + requestDto.getRecipientIdentifier()));

            UUID senderWalletId = requestDto.getSenderWalletId();
            UUID recipientWalletId = recipientWalletRef.getWalletId();

            if (senderWalletId.equals(recipientWalletId)) {
                throw new BusinessRuleException("Self-transfer is not allowed. Sender and recipient wallets cannot be identical.");
            }

            // 2. Acquire pessimistic write locks in deterministic sorted primary-key order (Deadlock Avoidance)
            Map<UUID, Wallet> lockedWallets = walletLockManager.acquireLocksAsMap(Arrays.asList(senderWalletId, recipientWalletId));

            Wallet senderWallet = lockedWallets.get(senderWalletId);
            Wallet recipientWallet = lockedWallets.get(recipientWalletId);

            if (senderWallet == null) {
                throw new BusinessRuleException("Sender wallet not found with ID: " + senderWalletId);
            }
            if (!senderWallet.isActive()) {
                throw new BusinessRuleException("Sender wallet is inactive: " + senderWalletId);
            }
            if (!recipientWallet.isActive()) {
                throw new BusinessRuleException("Recipient wallet is inactive: " + recipientWalletId);
            }

            // 3. Verify currency match
            if (!senderWallet.getCurrencyCode().equalsIgnoreCase(requestDto.getCurrencyCode())) {
                throw new BusinessRuleException(String.format("Currency mismatch. Sender wallet currency is %s but transfer currency is %s",
                        senderWallet.getCurrencyCode(), requestDto.getCurrencyCode()));
            }
            if (!recipientWallet.getCurrencyCode().equalsIgnoreCase(requestDto.getCurrencyCode())) {
                throw new BusinessRuleException(String.format("Currency mismatch. Recipient wallet currency is %s but transfer currency is %s",
                        recipientWallet.getCurrencyCode(), requestDto.getCurrencyCode()));
            }

            // 4. Verify sufficient funds
            long amount = requestDto.getAmountInMinorUnits();
            if (senderWallet.getAvailableBalanceInMinorUnits() < amount) {
                throw new InsufficientFundsException(String.format("Insufficient funds in sender wallet. Required: %d, Available: %d",
                        amount, senderWallet.getAvailableBalanceInMinorUnits()));
            }

            // 5. Create PENDING transaction record
            pendingTransaction = new Transaction();
            pendingTransaction.setSenderWallet(senderWallet);
            pendingTransaction.setRecipientWallet(recipientWallet);
            pendingTransaction.setAmountInMinorUnits(amount);
            pendingTransaction.setCurrencyCode(requestDto.getCurrencyCode());
            pendingTransaction.setStatus(TransferStatus.PENDING);
            pendingTransaction.setReference("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
            pendingTransaction.setNarration(requestDto.getNarration());

            Transaction savedTransaction = transactionRepository.save(pendingTransaction);

            // 6. Publish TransferInitiatedEvent
            eventPublisher.publishEvent(new TransferInitiatedEvent(this, savedTransaction));

            // 7. Perform atomic balance transfer
            senderWallet.setAvailableBalanceInMinorUnits(senderWallet.getAvailableBalanceInMinorUnits() - amount);
            recipientWallet.setAvailableBalanceInMinorUnits(recipientWallet.getAvailableBalanceInMinorUnits() + amount);

            // 8. Mark transaction COMPLETED
            savedTransaction.setStatus(TransferStatus.COMPLETED);
            Transaction completedTransaction = transactionRepository.save(savedTransaction);

            // 9. Publish TransferCompletedEvent (triggers double-entry ledger posting)
            eventPublisher.publishEvent(new TransferCompletedEvent(this, completedTransaction));

            transferSuccessCounter.increment();
            return transferMapper.mapToTransferResponseDto(completedTransaction);

        } catch (Exception ex) {
            transferFailedCounter.increment();
            log.error("Transfer failed for sender wallet ID: [{}]", requestDto.getSenderWalletId(), ex);

            if (pendingTransaction != null && pendingTransaction.getTransactionId() != null) {
                pendingTransaction.setStatus(TransferStatus.FAILED);
                transactionRepository.save(pendingTransaction);
                eventPublisher.publishEvent(new TransferFailedEvent(this, pendingTransaction, ex.getMessage()));
            }

            throw ex;
        } finally {
            sample.stop(transferExecutionTimer);
        }
    }
}
