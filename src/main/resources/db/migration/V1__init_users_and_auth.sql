CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(30) NOT NULL UNIQUE,
    user_account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    kyc_verification_tier VARCHAR(20) NOT NULL DEFAULT 'TIER_1',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email_address ON users(email_address);
CREATE INDEX idx_users_phone_number ON users(phone_number);