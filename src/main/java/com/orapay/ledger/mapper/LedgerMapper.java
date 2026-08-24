package com.orapay.ledger.mapper;

import com.orapay.ledger.dto.response.LedgerEntryResponseDto;
import com.orapay.ledger.model.LedgerEntry;
import org.springframework.stereotype.Component;

@Component
public class LedgerMapper {

    public LedgerEntryResponseDto mapToLedgerEntryResponseDto(LedgerEntry entry) {
        if (entry == null) return null;

        return LedgerEntryResponseDto.builder()
                .entryId(entry.getEntryId())
                .transactionId(entry.getTransactionId())
                .walletId(entry.getWalletId())
                .direction(entry.getDirection())
                .amountInMinorUnits(entry.getAmountInMinorUnits())
                .currencyCode(entry.getCurrencyCode())
                .allocationRole(entry.getAllocationRole())
                .reference(entry.getReference())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
