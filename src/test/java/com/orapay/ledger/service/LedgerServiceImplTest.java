package com.orapay.ledger.service;

import com.orapay.ledger.dto.response.LedgerEntryResponseDto;
import com.orapay.ledger.mapper.LedgerMapper;
import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import com.orapay.ledger.service.impl.LedgerServiceImpl;
import com.orapay.ledger.util.LedgerIntegrityVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Spy
    private LedgerMapper ledgerMapper;

    @Mock
    private LedgerIntegrityVerifier ledgerIntegrityVerifier;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    private UUID walletId;
    private LedgerEntry entry;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();

        entry = new LedgerEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setTransactionId(UUID.randomUUID());
        entry.setWalletId(walletId);
        entry.setEntryDirection(EntryDirection.DEBIT);
        entry.setAmountInMinorUnits(5000L);
        entry.setCurrencyCode("NGN");
        entry.setAllocationRole(AllocationRole.DEBIT);
        entry.setBalanceAfterPostingUnits(15000L);
    }

    @Test
    @DisplayName("Should return paginated ledger entries for wallet ID")
    void getLedgerEntriesForWallet_ShouldReturnPaginatedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<LedgerEntry> entryPage = new PageImpl<>(List.of(entry), pageable, 1);

        when(ledgerRepository.findByWalletId(walletId, pageable)).thenReturn(entryPage);

        Page<LedgerEntryResponseDto> result = ledgerService.getLedgerEntriesForWallet(walletId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        LedgerEntryResponseDto dto = result.getContent().get(0);
        assertThat(dto.getWalletId()).isEqualTo(walletId);
        assertThat(dto.getEntryDirection()).isEqualTo(EntryDirection.DEBIT);
        assertThat(dto.getBalanceAfterPostingUnits()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("Should delegate verifyTransactionIntegrity to LedgerIntegrityVerifier")
    void verifyTransactionIntegrity_ShouldDelegateToVerifier() {
        UUID transactionId = UUID.randomUUID();
        when(ledgerIntegrityVerifier.verifyTransactionIntegrity(ledgerService, transactionId)).thenReturn(true);

        boolean result = ledgerService.verifyTransactionIntegrity(transactionId);

        assertThat(result).isTrue();
        verify(ledgerIntegrityVerifier).verifyTransactionIntegrity(ledgerService, transactionId);
    }
}
