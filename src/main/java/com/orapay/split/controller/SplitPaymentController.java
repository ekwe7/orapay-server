package com.orapay.split.controller;

import com.orapay.common.dto.response.ApiResponse;
import com.orapay.split.dto.request.CreateSplitTemplateRequestDto;
import com.orapay.split.dto.request.MerchantCheckoutRequestDto;
import com.orapay.split.dto.request.SplitPaymentRequestDto;
import com.orapay.split.dto.response.SplitPaymentResponseDto;
import com.orapay.split.dto.response.SplitTemplateResponseDto;
import com.orapay.split.service.SplitPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Split Payments", description = "Endpoints for strategy-driven multi-party split payments and merchant checkout templates")
public class SplitPaymentController {

    private final SplitPaymentService splitPaymentService;

    @PostMapping("/split")
    @Operation(summary = "Ad-hoc multi-party split settlement checkout")
    public ResponseEntity<ApiResponse<SplitPaymentResponseDto>> processSplitPayment(
            @Valid @RequestBody SplitPaymentRequestDto requestDto
    ) {
        SplitPaymentResponseDto responseDto = splitPaymentService.processSplitPayment(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Split payment processed successfully", responseDto));
    }

    @PostMapping("/split/templates")
    @Operation(summary = "Register a merchant/school split agreement template")
    public ResponseEntity<ApiResponse<SplitTemplateResponseDto>> createSplitTemplate(
            @Valid @RequestBody CreateSplitTemplateRequestDto requestDto
    ) {
        SplitTemplateResponseDto responseDto = splitPaymentService.createSplitTemplate(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Split agreement template created successfully", responseDto));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Automated merchant checkout (e.g. school fee payment using pre-configured split template)")
    public ResponseEntity<ApiResponse<SplitPaymentResponseDto>> processMerchantCheckout(
            @Valid @RequestBody MerchantCheckoutRequestDto checkoutDto
    ) {
        SplitPaymentResponseDto responseDto = splitPaymentService.processMerchantCheckout(checkoutDto);
        return ResponseEntity.ok(ApiResponse.success("Merchant checkout payment processed and split successfully", responseDto));
    }
}
