package com.orapay.split.mapper;

import com.orapay.split.dto.response.SplitPaymentResponseDto;
import com.orapay.split.model.SplitOrder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class SplitPaymentMapper {

    public SplitPaymentResponseDto mapToSplitPaymentResponseDto(SplitOrder splitOrder) {
        if (splitOrder == null) return null;

        return SplitPaymentResponseDto.builder()
                .splitOrderId(splitOrder.getSplitOrderId())
                .payerWalletId(splitOrder.getPayerWallet() != null ? splitOrder.getPayerWallet().getWalletId() : null)
                .totalAmountInMinorUnits(splitOrder.getTotalAmountInMinorUnits())
                .currencyCode(splitOrder.getCurrencyCode())
                .status(splitOrder.getStatus() != null ? splitOrder.getStatus().name() : null)
                .allocations(splitOrder.getAllocations() != null ? splitOrder.getAllocations().stream()
                        .map(allocation -> SplitPaymentResponseDto.SplitAllocationResponseDto.builder()
                                .allocationId(allocation.getAllocationId())
                                .recipientWalletId(allocation.getRecipientWallet() != null ? allocation.getRecipientWallet().getWalletId() : null)
                                .allocatedAmountInMinorUnits(allocation.getAllocatedAmountInMinorUnits())
                                .build())
                        .collect(Collectors.toList()) : Collections.emptyList())
                .createdAt(splitOrder.getCreatedAt())
                .build();
    }
}
