package com.orapay.split.repository;

import com.orapay.split.model.SplitTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SplitTemplateRepository extends JpaRepository<SplitTemplate, UUID> {

    Optional<SplitTemplate> findByMerchantWallet_WalletIdAndFeeCategoryAndActiveTrue(UUID merchantWalletId, String feeCategory);

    Optional<SplitTemplate> findByMerchantWallet_WalletIdAndActiveTrue(UUID merchantWalletId);
}
