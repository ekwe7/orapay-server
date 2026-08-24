package com.orapay.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();

            try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS wallets (
                            wallet_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                            account_number VARCHAR(10) NOT NULL UNIQUE,
                            currency_code VARCHAR(3) NOT NULL DEFAULT 'NGN',
                            available_balance_minor_units BIGINT NOT NULL DEFAULT 0,
                            locked_balance_minor_units BIGINT NOT NULL DEFAULT 0,
                            version_lock BIGINT NOT NULL DEFAULT 0,
                            is_active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT chk_balances_non_negative CHECK (
                                available_balance_minor_units >= 0 AND locked_balance_minor_units >= 0
                            )
                        );
                        CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);
                        CREATE INDEX IF NOT EXISTS idx_wallets_account_number ON wallets(account_number);

                        CREATE TABLE IF NOT EXISTS idempotency_records (
                            idempotency_key VARCHAR(255) PRIMARY KEY,
                            request_path VARCHAR(255) NOT NULL,
                            response_status INT NOT NULL,
                            response_body TEXT,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                        );

                        CREATE TABLE IF NOT EXISTS transactions (
                            transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            sender_wallet_id UUID REFERENCES wallets(wallet_id),
                            recipient_wallet_id UUID REFERENCES wallets(wallet_id),
                            amount_minor_units BIGINT NOT NULL,
                            currency_code VARCHAR(3) NOT NULL DEFAULT 'NGN',
                            status VARCHAR(20) NOT NULL,
                            reference VARCHAR(255),
                            narration VARCHAR(255),
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                        );

                        CREATE TABLE IF NOT EXISTS split_orders (
                            split_order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            payer_wallet_id UUID REFERENCES wallets(wallet_id),
                            total_amount_minor_units BIGINT NOT NULL,
                            currency_code VARCHAR(3) NOT NULL DEFAULT 'NGN',
                            status VARCHAR(20) NOT NULL,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                        );

                        CREATE TABLE IF NOT EXISTS split_allocations (
                            allocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            split_order_id UUID REFERENCES split_orders(split_order_id) ON DELETE CASCADE,
                            recipient_wallet_id UUID REFERENCES wallets(wallet_id),
                            allocated_amount_minor_units BIGINT NOT NULL
                        );

                        CREATE TABLE IF NOT EXISTS ledger_entries (
                            entry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            transaction_id UUID NOT NULL,
                            wallet_id UUID NOT NULL REFERENCES wallets(wallet_id),
                            direction VARCHAR(10) NOT NULL,
                            amount_minor_units BIGINT NOT NULL,
                            currency_code VARCHAR(3) NOT NULL DEFAULT 'NGN',
                            allocation_role VARCHAR(20) NOT NULL,
                            reference VARCHAR(255),
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                        );
                        CREATE INDEX IF NOT EXISTS idx_ledger_entries_wallet_id ON ledger_entries(wallet_id);
                        CREATE INDEX IF NOT EXISTS idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);
                    """);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure database tables exist during Flyway migration", e);
            }
        };
    }
}
