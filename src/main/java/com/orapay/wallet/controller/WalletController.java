package com.orapay.wallet.controller;

import com.orapay.common.dto.response.ApiResponse;
import com.orapay.wallet.dto.request.CreateWalletRequestDto;
import com.orapay.wallet.dto.request.FundWalletRequestDto;
import com.orapay.wallet.dto.request.HoldFundsRequestDto;
import com.orapay.wallet.dto.response.WalletResponseDto;
import com.orapay.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "Endpoints for creating, retrieving, funding, and locking multi-currency balances")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a new wallet for a user")
    public ResponseEntity<ApiResponse<WalletResponseDto>> createWallet(
            @Valid @RequestBody CreateWalletRequestDto requestDto
    ) {
        WalletResponseDto walletResponse = walletService.createWallet(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", walletResponse));
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Fetch wallet & balance by walletId(UUID), 10-digit accountNumber, or phoneNumber")
    public ResponseEntity<ApiResponse<WalletResponseDto>> getWalletByIdentifier(
            @PathVariable String identifier
    ) {
        WalletResponseDto walletResponse = walletService.getWalletByIdentifier(identifier);
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", walletResponse));
    }

    @PostMapping("/{id}/fund")
    @Operation(summary = "Direct wallet funding endpoint")
    public ResponseEntity<ApiResponse<WalletResponseDto>> fundWallet(
            @PathVariable("id") UUID walletId,
            @Valid @RequestBody FundWalletRequestDto requestDto
    ) {
        WalletResponseDto walletResponse = walletService.fundWallet(walletId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("Wallet funded successfully", walletResponse));
    }

    @PostMapping("/{id}/hold")
    @Operation(summary = "Wallet fund reservation/hold endpoint")
    public ResponseEntity<ApiResponse<WalletResponseDto>> holdFunds(
            @PathVariable("id") UUID walletId,
            @Valid @RequestBody HoldFundsRequestDto requestDto
    ) {
        WalletResponseDto walletResponse = walletService.holdFunds(walletId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("Funds reserved on hold successfully", walletResponse));
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release funds on hold back to available balance")
    public ResponseEntity<ApiResponse<WalletResponseDto>> releaseHold(
            @PathVariable("id") UUID walletId,
            @Valid @RequestBody HoldFundsRequestDto requestDto
    ) {
        WalletResponseDto walletResponse = walletService.releaseHold(walletId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("Hold released successfully", walletResponse));
    }
}
