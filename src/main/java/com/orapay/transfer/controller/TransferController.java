package com.orapay.transfer.controller;

import com.orapay.common.dto.response.ApiResponse;
import com.orapay.transfer.dto.request.TransferRequestDto;
import com.orapay.transfer.dto.response.TransferResponseDto;
import com.orapay.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Endpoints for idempotent P2P transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Execute atomic P2P fund transfer")
    public ResponseEntity<ApiResponse<TransferResponseDto>> processTransfer(
            @Valid @RequestBody TransferRequestDto requestDto
    ) {
        TransferResponseDto responseDto = transferService.processTransfer(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Transfer processed successfully", responseDto));
    }
}
