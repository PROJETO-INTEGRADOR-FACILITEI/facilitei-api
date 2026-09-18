CREATE TABLE IF NOT EXISTS portfolio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trabalhador_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_trabalhador_id (trabalhador_id),
    CONSTRAINT fk_portfolio_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id)
);

CREATE TABLE IF NOT EXISTS portfolio_imagens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    portfolio_id BIGINT NOT NULL,
    url_imagem VARCHAR(500) NOT NULL,
    public_id VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_portfolio_imagens_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio (id)
);

SET @add_imagem_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE portfolio_imagens ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolio_imagens'
      AND column_name = 'id'
);

PREPARE add_imagem_id_statement FROM @add_imagem_id;
EXECUTE add_imagem_id_statement;
DEALLOCATE PREPARE add_imagem_id_statement;

SET @add_public_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE portfolio_imagens ADD COLUMN public_id VARCHAR(255) NULL AFTER url_imagem',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'portfolio_imagens'
      AND column_name = 'public_id'
);

PREPARE add_public_id_statement FROM @add_public_id;
EXECUTE add_public_id_statement;
DEALLOCATE PREPARE add_public_id_statement;
