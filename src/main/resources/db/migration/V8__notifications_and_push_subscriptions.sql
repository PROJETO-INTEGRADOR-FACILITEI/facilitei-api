CREATE TABLE app_notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    action_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES usuario (id),
    INDEX idx_notification_recipient_created (recipient_id, created_at),
    INDEX idx_notification_recipient_unread (recipient_id, read_at)
);

CREATE TABLE web_push_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(2048) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth_secret VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_push_subscription_user FOREIGN KEY (user_id) REFERENCES usuario (id),
    CONSTRAINT uk_push_subscription_endpoint UNIQUE (endpoint(512)),
    INDEX idx_push_subscription_user (user_id)
);
