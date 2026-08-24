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
