CREATE TABLE IF NOT EXISTS wallets (
        wallet_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE
  CASCADE,
        account_number VARCHAR(10) NOT NULL UNIQUE,
        currency_code VARCHAR(3) NOT NULL DEFAULT 'NGN',
        available_balance_minor_units BIGINT NOT NULL DEFAULT 0,
        locked_balance_minor_units BIGINT NOT NULL DEFAULT 0,
        version_lock BIGINT NOT NULL DEFAULT 0,
        is_active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT
  CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT
  CURRENT_TIMESTAMP,
        CONSTRAINT chk_balances_non_negative CHECK (
            available_balance_minor_units >= 0 AND
  locked_balance_minor_units >= 0
        )
    );
    
    CREATE INDEX idx_wallets_user_id ON wallets(user_id);
    CREATE INDEX idx_wallets_account_number ON
  wallets(account_number);