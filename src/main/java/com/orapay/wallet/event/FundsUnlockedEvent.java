package com.orapay.wallet.event;

import com.orapay.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class FundsUnlockedEvent extends BaseDomainEvent {

    private final UUID walletId;
    private final long amountInMinorUnits;
    private final String currencyCode;
    private final long updatedAvailableBalanceInMinorUnits;
    private final long updatedLockedBalanceInMinorUnits;
    private final String reference;

    public FundsUnlockedEvent(Object source, UUID walletId, long amountInMinorUnits, String currencyCode, long updatedAvailableBalanceInMinorUnits, long updatedLockedBalanceInMinorUnits, String reference) {
        super(source);
        this.walletId = walletId;
        this.amountInMinorUnits = amountInMinorUnits;
        this.currencyCode = currencyCode;
        this.updatedAvailableBalanceInMinorUnits = updatedAvailableBalanceInMinorUnits;
        this.updatedLockedBalanceInMinorUnits = updatedLockedBalanceInMinorUnits;
        this.reference = reference;
    }
}
