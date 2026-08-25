package com.orapay.split.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.split.model.SplitOrder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class SplitPaymentCompletedEvent extends BaseDomainEvent {

    private final UUID splitOrderId;
    private final UUID payerWalletId;
    private final long totalAmountInMinorUnits;
    private final String currencyCode;
    private final List<SplitAllocationEventData> allocations;

    public SplitPaymentCompletedEvent(Object source, SplitOrder splitOrder) {
        super(source);
        this.splitOrderId = splitOrder.getSplitOrderId();
        this.payerWalletId = splitOrder.getPayerWallet().getWalletId();
        this.totalAmountInMinorUnits = splitOrder.getTotalAmountInMinorUnits();
        this.currencyCode = splitOrder.getCurrencyCode();
        this.allocations = splitOrder.getAllocations().stream()
                .map(a -> new SplitAllocationEventData(a.getAllocationId(), a.getRecipientWallet().getWalletId(), a.getAllocatedAmountInMinorUnits()))
                .collect(Collectors.toList());
    }

    @Getter
    public static class SplitAllocationEventData {
        private final UUID allocationId;
        private final UUID recipientWalletId;
        private final long allocatedAmountInMinorUnits;

        public SplitAllocationEventData(UUID allocationId, UUID recipientWalletId, long allocatedAmountInMinorUnits) {
            this.allocationId = allocationId;
            this.recipientWalletId = recipientWalletId;
            this.allocatedAmountInMinorUnits = allocatedAmountInMinorUnits;
        }
    }
}
