package com.orapay.wallet.controller;
    
    import com.orapay.common.dto.ApiResponse;
    import com.orapay.wallet.dto.CreateWalletRequestDto;
    import com.orapay.wallet.dto.WalletResponseDto;
    import com.orapay.wallet.service.WalletService;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.tags.Tag;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    
    @RestController
    @RequestMapping("/api/wallets")
    @RequiredArgsConstructor
    @Tag(name = "Wallet Management", description = "Endpoints for creating and retrieving multi-currency balances and derived account numbers")
    public class WalletController {
    
        private final WalletService walletService;
    
        @PostMapping
        @Operation(summary = "Create a new wallet for a user")
        public ResponseEntity<ApiResponse<WalletResponseDto>>
  createWallet(
                @Valid @RequestBody CreateWalletRequestDto
  requestDto
        ) {
            WalletResponseDto walletResponse = walletService.
  createWallet(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Wallet created successfully", walletResponse));
        }
    
        @GetMapping("/{identifier}")
        @Operation(summary = "Fetch wallet & balance by walletId(UUID), 10-digit accountNumber, or phoneNumber")
        public ResponseEntity<ApiResponse<WalletResponseDto>>
  getWalletByIdentifier(
                @PathVariable String identifier
        ) {
            WalletResponseDto walletResponse = walletService.
  getWalletByIdentifier(identifier);
            return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", walletResponse));
        }
    }
