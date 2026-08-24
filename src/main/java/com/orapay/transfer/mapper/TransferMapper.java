package com.orapay.transfer.mapper;

import com.orapay.transfer.dto.response.TransferResponseDto;
import com.orapay.transfer.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponseDto mapToTransferResponseDto(Transaction transaction) {
        if (transaction == null) return null;

        return TransferResponseDto.builder()
                .transactionId(transaction.getTransactionId())
                .senderWalletId(transaction.getSenderWallet() != null ? transaction.getSenderWallet().getWalletId() : null)
                .recipientWalletId(transaction.getRecipientWallet() != null ? transaction.getRecipientWallet().getWalletId() : null)
                .amountInMinorUnits(transaction.getAmountInMinorUnits())
                .currencyCode(transaction.getCurrencyCode())
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
                .reference(transaction.getReference())
                .narration(transaction.getNarration())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
