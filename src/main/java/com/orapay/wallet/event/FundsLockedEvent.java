package com.orapay.wallet.event;

import com.orapay.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class FundsLockedEvent extends BaseDomainEvent {

    private final UUID walletId;
    private final long amountInMinorUnits;
    private final String currencyCode;
    private final long remainingAvailableBalanceInMinorUnits;
    private final long totalLockedBalanceInMinorUnits;
    private final String reference;

    public FundsLockedEvent(Object source, UUID walletId, long amountInMinorUnits, String currencyCode, long remainingAvailableBalanceInMinorUnits, long totalLockedBalanceInMinorUnits, String reference) {
        super(source);
        this.walletId = walletId;
        this.amountInMinorUnits = amountInMinorUnits;
        this.currencyCode = currencyCode;
        this.remainingAvailableBalanceInMinorUnits = remainingAvailableBalanceInMinorUnits;
        this.totalLockedBalanceInMinorUnits = totalLockedBalanceInMinorUnits;
        this.reference = reference;
    }
}
