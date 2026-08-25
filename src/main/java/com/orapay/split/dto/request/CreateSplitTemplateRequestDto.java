package com.orapay.split.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSplitTemplateRequestDto {

    @NotNull(message = "Merchant wallet ID is mandatory")
    private UUID merchantWalletId;

    @NotBlank(message = "Template name is mandatory")
    private String templateName;

    @NotBlank(message = "Fee category is mandatory")
    private String feeCategory;

    @NotEmpty(message = "Rules cannot be empty")
    private List<TemplateRuleDto> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateRuleDto {
        @NotNull(message = "Recipient wallet ID is mandatory")
        private UUID recipientWalletId;
        private String recipientName;
        private BigDecimal percentage;
        private Long fixedAmountInMinorUnits;
    }
}
