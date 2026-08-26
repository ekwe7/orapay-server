package com.orapay.ledger.util;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.ledger.event.LedgerDiscrepancyDetectedEvent;
import com.orapay.ledger.model.AllocationRole;
import com.orapay.ledger.model.EntryDirection;
import com.orapay.ledger.model.LedgerEntry;
import com.orapay.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerIntegrityVerifierTest {

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private LedgerIntegrityVerifier verifier;

    private UUID transactionId;
    private LedgerEntry debitEntry;
    private LedgerEntry creditEntry;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();

        debitEntry = new LedgerEntry();
        debitEntry.setTransactionId(transactionId);
        debitEntry.setWalletId(UUID.randomUUID());
        debitEntry.setEntryDirection(EntryDirection.DEBIT);
        debitEntry.setAmountInMinorUnits(15000L);
        debitEntry.setCurrencyCode("NGN");
        debitEntry.setAllocationRole(AllocationRole.DEBIT);

        creditEntry = new LedgerEntry();
        creditEntry.setTransactionId(transactionId);
        creditEntry.setWalletId(UUID.randomUUID());
        creditEntry.setEntryDirection(EntryDirection.CREDIT);
        creditEntry.setAmountInMinorUnits(15000L);
        creditEntry.setCurrencyCode("NGN");
        creditEntry.setAllocationRole(AllocationRole.CREDIT);
    }

    @Test
    @DisplayName("validateAndVerifyBalanced returns true when sum(Debits) - sum(Credits) == 0")
    void validateAndVerifyBalanced_ShouldPassWhenBalanced() {
        boolean result = verifier.validateAndVerifyBalanced(this, transactionId, List.of(debitEntry, creditEntry));
        assertThat(result).isTrue();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("validateAndVerifyBalanced throws BusinessRuleException and emits LedgerDiscrepancyDetectedEvent when imbalanced")
    void validateAndVerifyBalanced_ShouldFailAndEmitEventWhenImbalanced() {
        creditEntry.setAmountInMinorUnits(14000L); // imbalanced by 1000

        assertThatThrownBy(() -> verifier.validateAndVerifyBalanced(this, transactionId, List.of(debitEntry, creditEntry)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ledger integrity violation");

        verify(eventPublisher).publishEvent(any(LedgerDiscrepancyDetectedEvent.class));
    }

    @Test
    @DisplayName("verifyTransactionIntegrity returns true when stored entries are balanced")
    void verifyTransactionIntegrity_ShouldReturnTrueWhenBalanced() {
        when(ledgerRepository.findByTransactionId(transactionId)).thenReturn(List.of(debitEntry, creditEntry));

        boolean result = verifier.verifyTransactionIntegrity(this, transactionId);

        assertThat(result).isTrue();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("verifyTransactionIntegrity returns false and emits event when stored entries are imbalanced")
    void verifyTransactionIntegrity_ShouldReturnFalseAndEmitEventWhenImbalanced() {
        creditEntry.setAmountInMinorUnits(10000L); // imbalanced
        when(ledgerRepository.findByTransactionId(transactionId)).thenReturn(List.of(debitEntry, creditEntry));

        boolean result = verifier.verifyTransactionIntegrity(this, transactionId);

        assertThat(result).isFalse();
        verify(eventPublisher).publishEvent(any(LedgerDiscrepancyDetectedEvent.class));
    }
}
