package com.orapay.split.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitTemplateResponseDto {

    private UUID templateId;
    private UUID merchantWalletId;
    private String templateName;
    private String feeCategory;
    private boolean active;
    private List<TemplateRuleResponseDto> rules;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateRuleResponseDto {
        private UUID ruleId;
        private UUID recipientWalletId;
        private String recipientName;
        private BigDecimal percentage;
        private Long fixedAmountInMinorUnits;
    }
}
