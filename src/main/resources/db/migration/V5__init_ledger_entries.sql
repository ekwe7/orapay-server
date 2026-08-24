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
