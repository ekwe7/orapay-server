package com.orapay.wallet.event;

import com.orapay.common.event.BaseDomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class WalletFrozenEvent extends BaseDomainEvent {

    private final UUID walletId;
    private final String accountNumber;
    private final String reasonDescription;

    public WalletFrozenEvent(Object source, UUID walletId, String accountNumber, String reasonDescription) {
        super(source);
        this.walletId = walletId;
        this.accountNumber = accountNumber;
        this.reasonDescription = reasonDescription;
    }
}
