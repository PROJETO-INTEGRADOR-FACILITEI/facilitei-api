-- Punições temporárias: NULL em suspended_until significa suspensão por tempo indeterminado.
ALTER TABLE usuario
    ADD COLUMN suspended_until DATETIME(6) NULL;

-- Serviços criados pelo próprio profissional (além das categorias fixas).
CREATE TABLE servico_oferecido (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trabalhador_id BIGINT NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    preco DECIMAL(10, 2) NULL,
    tipo_servico VARCHAR(255) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_servico_oferecido_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id)
);

CREATE INDEX idx_servico_oferecido_trabalhador ON servico_oferecido (trabalhador_id, ativo);

ALTER TABLE solicitacao_servico
    ADD COLUMN servico_oferecido_id BIGINT NULL,
    ADD COLUMN titulo VARCHAR(150) NULL,
    ADD COLUMN motivo_cancelamento VARCHAR(500) NULL,
    ADD CONSTRAINT fk_solicitacao_servico_oferecido FOREIGN KEY (servico_oferecido_id) REFERENCES servico_oferecido (id);

ALTER TABLE servico
    ADD COLUMN motivo_cancelamento VARCHAR(500) NULL;
