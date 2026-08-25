package com.orapay.split.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.split.model.SplitOrder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SplitPaymentInitiatedEvent extends BaseDomainEvent {

    private final UUID splitOrderId;
    private final UUID payerWalletId;
    private final long totalAmountInMinorUnits;
    private final String currencyCode;

    public SplitPaymentInitiatedEvent(Object source, SplitOrder splitOrder) {
        super(source);
        this.splitOrderId = splitOrder.getSplitOrderId();
        this.payerWalletId = splitOrder.getPayerWallet() != null ? splitOrder.getPayerWallet().getWalletId() : null;
        this.totalAmountInMinorUnits = splitOrder.getTotalAmountInMinorUnits();
        this.currencyCode = splitOrder.getCurrencyCode();
    }
}
