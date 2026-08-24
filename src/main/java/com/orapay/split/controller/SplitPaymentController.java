package com.orapay.split.controller;

import com.orapay.common.dto.response.ApiResponse;
import com.orapay.split.dto.request.SplitPaymentRequestDto;
import com.orapay.split.dto.response.SplitPaymentResponseDto;
import com.orapay.split.service.SplitPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Split Payments", description = "Endpoints for strategy-driven multi-party split payments")
public class SplitPaymentController {

    private final SplitPaymentService splitPaymentService;

    @PostMapping("/split")
    @Operation(summary = "Multi-party split settlement checkout")
    public ResponseEntity<ApiResponse<SplitPaymentResponseDto>> processSplitPayment(
            @Valid @RequestBody SplitPaymentRequestDto requestDto
    ) {
        SplitPaymentResponseDto responseDto = splitPaymentService.processSplitPayment(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Split payment processed successfully", responseDto));
    }
}
