CREATE TABLE support_case (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    protocol VARCHAR(32) NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT NULL,
    service_id BIGINT NULL,
    assigned_admin_id BIGINT NULL,
    subject VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    resolution TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    closed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_support_case_protocol UNIQUE (protocol),
    CONSTRAINT fk_support_case_reporter FOREIGN KEY (reporter_id) REFERENCES usuario (id),
    CONSTRAINT fk_support_case_reported_user FOREIGN KEY (reported_user_id) REFERENCES usuario (id),
    CONSTRAINT fk_support_case_service FOREIGN KEY (service_id) REFERENCES servico (id),
    CONSTRAINT fk_support_case_assignee FOREIGN KEY (assigned_admin_id) REFERENCES usuario (id),
    INDEX idx_support_case_reporter_updated (reporter_id, updated_at),
    INDEX idx_support_case_queue (status, priority, updated_at),
    INDEX idx_support_case_assignee (assigned_admin_id, status)
);

CREATE TABLE support_case_evidence (
    case_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    url VARCHAR(500) NOT NULL,
    PRIMARY KEY (case_id, display_order),
    CONSTRAINT fk_support_evidence_case FOREIGN KEY (case_id) REFERENCES support_case (id) ON DELETE CASCADE
);

CREATE TABLE support_message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_support_message_case FOREIGN KEY (case_id) REFERENCES support_case (id) ON DELETE CASCADE,
    CONSTRAINT fk_support_message_author FOREIGN KEY (author_id) REFERENCES usuario (id),
    INDEX idx_support_message_case_created (case_id, created_at)
);

CREATE TABLE support_case_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_support_history_case FOREIGN KEY (case_id) REFERENCES support_case (id) ON DELETE CASCADE,
    CONSTRAINT fk_support_history_actor FOREIGN KEY (actor_id) REFERENCES usuario (id),
    INDEX idx_support_history_case_created (case_id, created_at)
);
