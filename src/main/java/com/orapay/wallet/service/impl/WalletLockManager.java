package com.orapay.wallet.service.impl;

import com.orapay.common.exception.BusinessRuleException;
import com.orapay.common.exception.ConcurrencyConflictException;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WalletLockManager {

    private final WalletRepository walletRepository;
    private final MeterRegistry meterRegistry;
    private final Timer lockWaitTimer;

    public WalletLockManager(WalletRepository walletRepository, MeterRegistry meterRegistry) {
        this.walletRepository = walletRepository;
        this.meterRegistry = meterRegistry;
        this.lockWaitTimer = Timer.builder("wallet.lock.wait.duration")
                .description("Duration spent acquiring pessimistic wallet write locks")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public Wallet acquireLock(UUID walletId) {
        List<Wallet> wallets = acquireLocks(Collections.singletonList(walletId));
        if (wallets.isEmpty()) {
            throw new BusinessRuleException("Wallet not found with ID: " + walletId);
        }
        return wallets.get(0);
    }

    public List<Wallet> acquireLocks(Collection<UUID> walletIds) {
        if (walletIds == null || walletIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> sortedIds = walletIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        Timer.Sample sample = Timer.start(meterRegistry);
        Map<UUID, Wallet> lockedMap = new HashMap<>();
        try {
            for (UUID id : sortedIds) {
                Wallet wallet = walletRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new BusinessRuleException("Wallet not found with ID: " + id));
                lockedMap.put(id, wallet);
            }
        } catch (PessimisticLockingFailureException | PessimisticLockException | LockTimeoutException ex) {
            throw new ConcurrencyConflictException("Failed to acquire pessimistic write lock due to lock contention/timeout", ex);
        } finally {
            sample.stop(lockWaitTimer);
        }

        return sortedIds.stream()
                .map(lockedMap::get)
                .collect(Collectors.toList());
    }

    public Map<UUID, Wallet> acquireLocksAsMap(Collection<UUID> walletIds) {
        List<Wallet> lockedList = acquireLocks(walletIds);
        return lockedList.stream()
                .collect(Collectors.toMap(Wallet::getWalletId, Function.identity()));
    }
}
