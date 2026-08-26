package com.orapay.ledger.service;

import com.orapay.ledger.dto.response.LedgerEntryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LedgerService {

    Page<LedgerEntryResponseDto> getLedgerEntriesForWallet(UUID walletId, Pageable pageable);

    boolean verifyTransactionIntegrity(UUID transactionId);
}
