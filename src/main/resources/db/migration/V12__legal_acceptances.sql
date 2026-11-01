CREATE TABLE legal_acceptance (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(30) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    document_version VARCHAR(50) NOT NULL,
    document_sha256 VARCHAR(64) NOT NULL,
    acceptance_context VARCHAR(30) NOT NULL,
    accepted_at DATETIME(6) NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    user_agent_hash VARCHAR(64) NOT NULL,
    adult_confirmed BOOLEAN NOT NULL,
    CONSTRAINT uk_legal_acceptance_document UNIQUE (
        user_id, user_role, document_type, document_version, document_sha256
    ),
    CONSTRAINT ck_legal_acceptance_role CHECK (user_role IN ('cliente', 'trabalhador')),
    CONSTRAINT ck_legal_acceptance_adult CHECK (adult_confirmed = TRUE)
);

CREATE INDEX idx_legal_acceptance_user
    ON legal_acceptance (user_id, user_role);
