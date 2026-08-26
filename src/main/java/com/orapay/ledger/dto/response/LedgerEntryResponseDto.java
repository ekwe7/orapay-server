package com.orapay.ledger.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("entryDirection")
    private EntryDirection entryDirection;
    private long amountInMinorUnits;
    private String currencyCode;
    private AllocationRole allocationRole;
    private String reference;
    private Long balanceAfterPostingUnits;
    private Instant createdAt;

    @JsonProperty("direction")
    public EntryDirection getDirection() {
        return entryDirection;
    }
}
