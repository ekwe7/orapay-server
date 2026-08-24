CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_path VARCHAR(255) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
