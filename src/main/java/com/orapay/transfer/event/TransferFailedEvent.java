package com.orapay.transfer.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.transfer.model.Transaction;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TransferFailedEvent extends BaseDomainEvent {

    private final UUID transactionId;
    private final UUID senderWalletId;
    private final UUID recipientWalletId;
    private final long amountInMinorUnits;
    private final String reason;

    public TransferFailedEvent(Object source, Transaction transaction, String reason) {
        super(source);
        this.transactionId = transaction != null ? transaction.getTransactionId() : null;
        this.senderWalletId = transaction != null && transaction.getSenderWallet() != null ? transaction.getSenderWallet().getWalletId() : null;
        this.recipientWalletId = transaction != null && transaction.getRecipientWallet() != null ? transaction.getRecipientWallet().getWalletId() : null;
        this.amountInMinorUnits = transaction != null ? transaction.getAmountInMinorUnits() : 0L;
        this.reason = reason;
    }
}
