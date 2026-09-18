ALTER TABLE portfolio_imagens
    ADD COLUMN tipo_servico VARCHAR(64) NULL AFTER public_id;

CREATE INDEX idx_portfolio_imagens_tipo_servico
    ON portfolio_imagens (portfolio_id, tipo_servico);
