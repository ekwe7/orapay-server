package com.orapay.ledger.repository;

import com.orapay.ledger.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByWalletId(UUID walletId, Pageable pageable);

    List<LedgerEntry> findByTransactionId(UUID transactionId);
}
