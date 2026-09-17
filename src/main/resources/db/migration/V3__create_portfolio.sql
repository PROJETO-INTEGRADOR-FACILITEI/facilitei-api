CREATE TABLE portfolio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trabalhador_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_trabalhador_id (trabalhador_id),
    CONSTRAINT fk_portfolio_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id)
);

CREATE TABLE portfolio_imagens (
    portfolio_id BIGINT NOT NULL,
    url_imagem VARCHAR(500) NULL,
    CONSTRAINT fk_portfolio_imagens_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio (id)
);
