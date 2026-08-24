package com.orapay.wallet.repository;
    
    import com.orapay.wallet.model.Wallet;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;
    
    import java.util.Optional;
    import java.util.UUID;
    
    @Repository
    public interface WalletRepository extends
  JpaRepository<Wallet, UUID> {
    
        Optional<Wallet> findByAccountNumber(String
  accountNumber);
    
        Optional<Wallet> findByUser_UserUniqueId(UUID userId);
    
        boolean existsByUser_UserUniqueIdAndCurrencyCode(UUID userId, String
  currencyCode);
    
        boolean existsByAccountNumber(String accountNumber);
    
        /**
         * Polymorphic Recipient Identifier Resolver:
         * Attempts lookup via walletId (UUID), 10-digit
  accountNumber, or E.164 phoneNumber
         */
        @Query("""
            SELECT w FROM Wallet w
            JOIN w.user u
            WHERE w.accountNumber = :identifier
               OR u.phoneNumber = :identifier
               OR (CAST(:identifier AS uuid) IS NOT NULL AND w.
  walletId = CAST(:identifier AS uuid))
        """)
        Optional<Wallet>
  resolveRecipientByIdentifier(@Param("identifier") String
  identifier);
    }
