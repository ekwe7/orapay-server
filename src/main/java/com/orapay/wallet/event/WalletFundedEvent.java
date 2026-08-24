package com.orapay.wallet.event;

import com.orapay.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class WalletFundedEvent extends BaseDomainEvent {

    private final UUID walletId;
    private final long amountInMinorUnits;
    private final String currencyCode;
    private final long newAvailableBalanceInMinorUnits;
    private final String reference;

    public WalletFundedEvent(Object source, UUID walletId, long amountInMinorUnits, String currencyCode, long newAvailableBalanceInMinorUnits, String reference) {
        super(source);
        this.walletId = walletId;
        this.amountInMinorUnits = amountInMinorUnits;
        this.currencyCode = currencyCode;
        this.newAvailableBalanceInMinorUnits = newAvailableBalanceInMinorUnits;
        this.reference = reference;
    }
}
