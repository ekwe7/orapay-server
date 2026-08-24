package com.orapay.ledger.controller;

import com.orapay.common.dto.response.ApiResponse;
import com.orapay.ledger.dto.response.LedgerEntryResponseDto;
import com.orapay.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Double-Entry Ledger Audit", description = "Endpoints for fetching immutable ledger audit records")
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/{id}/ledger")
    @Operation(summary = "Fetch paginated double-entry ledger audit trail for a wallet")
    public ResponseEntity<ApiResponse<Page<LedgerEntryResponseDto>>> getLedgerEntriesForWallet(
            @PathVariable("id") UUID walletId,
            Pageable pageable
    ) {
        Page<LedgerEntryResponseDto> ledgerEntries = ledgerService.getLedgerEntriesForWallet(walletId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Ledger entries retrieved successfully", ledgerEntries));
    }
}
