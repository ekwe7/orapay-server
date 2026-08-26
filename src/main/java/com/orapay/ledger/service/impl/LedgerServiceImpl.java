package com.orapay.ledger.service.impl;

import com.orapay.ledger.dto.response.LedgerEntryResponseDto;
import com.orapay.ledger.mapper.LedgerMapper;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.ledger.service.LedgerService;
import com.orapay.ledger.util.LedgerIntegrityVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;
    private final LedgerMapper ledgerMapper;
    private final LedgerIntegrityVerifier ledgerIntegrityVerifier;

    @Override
    @Transactional(readOnly = true)
    public Page<LedgerEntryResponseDto> getLedgerEntriesForWallet(UUID walletId, Pageable pageable) {
        return ledgerRepository.findByWalletId(walletId, pageable)
                .map(ledgerMapper::mapToLedgerEntryResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyTransactionIntegrity(UUID transactionId) {
        return ledgerIntegrityVerifier.verifyTransactionIntegrity(this, transactionId);
    }
}
