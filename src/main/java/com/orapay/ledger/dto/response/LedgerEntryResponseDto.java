package com.orapay.ledger.dto.response;

import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryResponseDto {

    private UUID entryId;
    private UUID transactionId;
    private UUID walletId;
    private EntryDirection direction;
    private long amountInMinorUnits;
    private String currencyCode;
    private AllocationRole allocationRole;
    private String reference;
    private Instant createdAt;
}
