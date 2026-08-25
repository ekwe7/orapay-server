CREATE TABLE IF NOT EXISTS split_templates (
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_wallet_id UUID NOT NULL REFERENCES wallets(wallet_id),
    template_name VARCHAR(100) NOT NULL,
    fee_category VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS split_template_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES split_templates(template_id) ON DELETE CASCADE,
    recipient_wallet_id UUID NOT NULL REFERENCES wallets(wallet_id),
    recipient_name VARCHAR(100),
    percentage NUMERIC(5, 2),
    fixed_amount_minor_units BIGINT
);

CREATE INDEX IF NOT EXISTS idx_split_templates_merchant ON split_templates(merchant_wallet_id, fee_category);
