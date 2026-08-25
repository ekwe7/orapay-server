package com.orapay.split.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.split.model.SplitOrder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SplitPaymentFailedEvent extends BaseDomainEvent {

    private final UUID splitOrderId;
    private final UUID payerWalletId;
    private final long totalAmountInMinorUnits;
    private final String reason;

    public SplitPaymentFailedEvent(Object source, SplitOrder splitOrder, String reason) {
        super(source);
        this.splitOrderId = splitOrder != null ? splitOrder.getSplitOrderId() : null;
        this.payerWalletId = splitOrder != null && splitOrder.getPayerWallet() != null ? splitOrder.getPayerWallet().getWalletId() : null;
        this.totalAmountInMinorUnits = splitOrder != null ? splitOrder.getTotalAmountInMinorUnits() : 0L;
        this.reason = reason;
    }
}
