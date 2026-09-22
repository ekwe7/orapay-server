-- Flyway Migration V10: Notification Subsystem Logs
CREATE TABLE notification_logs (
    notification_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL, -- 'EMAIL' or 'SMS'
    recipient_address VARCHAR(255) NOT NULL, -- Email string or E.164 Phone number
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'SENT', 'FAILED'
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_notification_user_id ON notification_logs(user_id);
-- Index for monitoring failed notifications pending background retries
CREATE INDEX idx_notification_status ON notification_logs(status);