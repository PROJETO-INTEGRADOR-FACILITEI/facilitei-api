CREATE TABLE assinatura_prestador (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    trabalhador_id BIGINT NOT NULL,
    checkout_id VARCHAR(100) NULL,
    checkout_url VARCHAR(500) NULL,
    subscription_id VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    amount_cents BIGINT NULL,
    active_until DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_assinatura_trabalhador UNIQUE (trabalhador_id),
    CONSTRAINT uk_assinatura_checkout UNIQUE (checkout_id),
    CONSTRAINT uk_assinatura_subscription UNIQUE (subscription_id),
    CONSTRAINT fk_assinatura_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id)
);

CREATE TABLE evento_abacatepay (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    received_at DATETIME(6) NOT NULL
);
