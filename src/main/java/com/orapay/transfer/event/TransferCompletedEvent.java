package com.orapay.transfer.event;

import com.orapay.common.event.BaseDomainEvent;
import com.orapay.transfer.model.Transaction;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TransferCompletedEvent extends BaseDomainEvent {

    private final UUID transactionId;
    private final UUID senderWalletId;
    private final UUID recipientWalletId;
    private final long amountInMinorUnits;
    private final String currencyCode;
    private final String reference;

    public TransferCompletedEvent(Object source, Transaction transaction) {
        super(source);
        this.transactionId = transaction.getTransactionId();
        this.senderWalletId = transaction.getSenderWallet() != null ? transaction.getSenderWallet().getWalletId() : null;
        this.recipientWalletId = transaction.getRecipientWallet() != null ? transaction.getRecipientWallet().getWalletId() : null;
        this.amountInMinorUnits = transaction.getAmountInMinorUnits();
        this.currencyCode = transaction.getCurrencyCode();
        this.reference = transaction.getReference();
    }
}
