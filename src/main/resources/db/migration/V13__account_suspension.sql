ALTER TABLE usuario
    ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN suspended_at DATETIME(6) NULL,
    ADD COLUMN suspension_reason TEXT NULL;

CREATE INDEX idx_usuario_account_status ON usuario (account_status);
