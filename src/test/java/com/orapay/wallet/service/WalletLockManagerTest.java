package com.orapay.wallet.service;

import com.orapay.common.exception.ConcurrencyConflictException;
import com.orapay.wallet.model.Wallet;
import com.orapay.wallet.repository.WalletRepository;
import com.orapay.wallet.service.impl.WalletLockManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletLockManagerTest {

    @Mock
    private WalletRepository walletRepository;

    private MeterRegistry meterRegistry;
    private WalletLockManager walletLockManager;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        walletLockManager = new WalletLockManager(walletRepository, meterRegistry);
    }

    @Test
    @DisplayName("Should acquire locks strictly in sorted UUID natural order to prevent deadlocks")
    void acquireLocks_ShouldLockInSortedUuidOrder() {
        UUID id1 = UUID.fromString("eeeeeeee-4444-4444-4444-eeeeeeeeeeee");
        UUID id2 = UUID.fromString("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa");
        UUID id3 = UUID.fromString("cccccccc-3333-3333-3333-cccccccccccc");

        Wallet w1 = new Wallet();
        w1.setWalletId(id1);
        Wallet w2 = new Wallet();
        w2.setWalletId(id2);
        Wallet w3 = new Wallet();
        w3.setWalletId(id3);

        given(walletRepository.findByIdForUpdate(id1)).willReturn(Optional.of(w1));
        given(walletRepository.findByIdForUpdate(id2)).willReturn(Optional.of(w2));
        given(walletRepository.findByIdForUpdate(id3)).willReturn(Optional.of(w3));

        List<Wallet> lockedWallets = walletLockManager.acquireLocks(List.of(id1, id2, id3));

        assertThat(lockedWallets).hasSize(3);

        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(walletRepository, times(3)).findByIdForUpdate(uuidCaptor.capture());

        List<UUID> capturedUuids = uuidCaptor.getAllValues();
        assertThat(capturedUuids).containsExactly(id2, id3, id1);

        assertThat(meterRegistry.find("wallet.lock.wait.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("wallet.lock.wait.duration").timer().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should wrap lock failure exception in ConcurrencyConflictException")
    void acquireLocks_ShouldThrowConcurrencyConflictException_OnPessimisticLockFailure() {
        UUID id = UUID.randomUUID();
        given(walletRepository.findByIdForUpdate(id))
                .willThrow(new PessimisticLockingFailureException("Database lock timeout"));

        assertThatThrownBy(() -> walletLockManager.acquireLock(id))
                .isInstanceOf(ConcurrencyConflictException.class)
                .hasMessageContaining("Failed to acquire pessimistic write lock");
    }
}
