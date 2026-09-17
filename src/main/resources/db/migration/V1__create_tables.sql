-- Schema inicial do Facilitei, refletindo o estado das entidades JPA
-- antes da adição do campo preco em Servico e da entidade Portfolio.
-- As tabelas são criadas sem FKs inline por causa das referências
-- circulares entre servico <-> avaliacao_servico e servico <-> solicitacao_servico;
-- todas as constraints de FK são adicionadas ao final do arquivo.

CREATE TABLE endereco (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rua VARCHAR(255) NOT NULL,
    numero VARCHAR(255) NOT NULL,
    bairro VARCHAR(255) NOT NULL,
    cidade VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    cep VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    url_foto VARCHAR(255) NULL,
    endereco_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_email (email)
);

CREATE TABLE cliente (
    cliente_id BIGINT NOT NULL,
    nota_cliente DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (cliente_id)
);

CREATE TABLE trabalhador (
    id BIGINT NOT NULL,
    nota_trabalhador DOUBLE NULL DEFAULT 0,
    disponibilidade VARCHAR(255) NULL,
    sobre VARCHAR(200) NULL,
    servico_principal VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE trabalhador_habilidades (
    trabalhador_id BIGINT NOT NULL,
    habilidade VARCHAR(255) NOT NULL
);

CREATE TABLE servico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    avaliacao_id BIGINT NULL,
    solicitacao_id BIGINT NULL,
    tipo_servico VARCHAR(255) NOT NULL,
    trabalhador_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    status_servico VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_servico_avaliacao_id (avaliacao_id),
    UNIQUE KEY uk_servico_solicitacao_id (solicitacao_id)
);

CREATE TABLE avaliacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nota INT NOT NULL,
    data DATETIME NOT NULL,
    comentario VARCHAR(1000) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE avaliacao_fotos (
    avaliacao_id BIGINT NOT NULL,
    url_foto VARCHAR(500) NULL
);

CREATE TABLE avaliacao_servico (
    id BIGINT NOT NULL,
    cliente_id BIGINT NULL,
    servico_id BIGINT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE avaliacao_cliente (
    id BIGINT NOT NULL,
    trabalhador_id BIGINT NULL,
    cliente_id BIGINT NULL,
    servico_id BIGINT NULL,
    media_cliente DOUBLE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE avaliacao_trabalhador (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trabalhador_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    nota INT NOT NULL,
    data DATETIME NOT NULL,
    comentario VARCHAR(1000) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE avaliacao_trabalhador_fotos (
    avaliacao_id BIGINT NOT NULL,
    url_foto VARCHAR(500) NULL
);

CREATE TABLE solicitacao_servico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    trabalhador_id BIGINT NOT NULL,
    servico_id BIGINT NULL,
    tipo_servico VARCHAR(255) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    data_solicitacao DATETIME NOT NULL,
    status_solicitacao VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solicitacao_servico_id (servico_id)
);

CREATE TABLE mensagem (
    id BIGINT NOT NULL AUTO_INCREMENT,
    servico_id BIGINT NOT NULL,
    remetente VARCHAR(255) NOT NULL,
    conteudo TEXT NULL,
    tipo VARCHAR(255) NULL,
    url_arquivo VARCHAR(255) NULL,
    data_envio DATETIME NULL,
    PRIMARY KEY (id)
);

CREATE TABLE password_reset_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    data_expiracao DATETIME NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_token_token (token)
);

-- ===================== Foreign keys =====================

ALTER TABLE usuario
    ADD CONSTRAINT fk_usuario_endereco FOREIGN KEY (endereco_id) REFERENCES endereco (id);

ALTER TABLE cliente
    ADD CONSTRAINT fk_cliente_usuario FOREIGN KEY (cliente_id) REFERENCES usuario (id);

ALTER TABLE trabalhador
    ADD CONSTRAINT fk_trabalhador_usuario FOREIGN KEY (id) REFERENCES usuario (id);

ALTER TABLE trabalhador_habilidades
    ADD CONSTRAINT fk_trabalhador_habilidades_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id);

ALTER TABLE servico
    ADD CONSTRAINT fk_servico_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id),
    ADD CONSTRAINT fk_servico_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id),
    ADD CONSTRAINT fk_servico_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacao_servico (id),
    ADD CONSTRAINT fk_servico_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES solicitacao_servico (id);

ALTER TABLE avaliacao_fotos
    ADD CONSTRAINT fk_avaliacao_fotos_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacao (id);

ALTER TABLE avaliacao_servico
    ADD CONSTRAINT fk_avaliacao_servico_avaliacao FOREIGN KEY (id) REFERENCES avaliacao (id),
    ADD CONSTRAINT fk_avaliacao_servico_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id),
    ADD CONSTRAINT fk_avaliacao_servico_servico FOREIGN KEY (servico_id) REFERENCES servico (id);

ALTER TABLE avaliacao_cliente
    ADD CONSTRAINT fk_avaliacao_cliente_avaliacao FOREIGN KEY (id) REFERENCES avaliacao (id),
    ADD CONSTRAINT fk_avaliacao_cliente_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id),
    ADD CONSTRAINT fk_avaliacao_cliente_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id);

ALTER TABLE avaliacao_trabalhador
    ADD CONSTRAINT fk_avaliacao_trabalhador_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id),
    ADD CONSTRAINT fk_avaliacao_trabalhador_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id);

ALTER TABLE avaliacao_trabalhador_fotos
    ADD CONSTRAINT fk_avaliacao_trabalhador_fotos_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacao_trabalhador (id);

ALTER TABLE solicitacao_servico
    ADD CONSTRAINT fk_solicitacao_servico_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id),
    ADD CONSTRAINT fk_solicitacao_servico_trabalhador FOREIGN KEY (trabalhador_id) REFERENCES trabalhador (id),
    ADD CONSTRAINT fk_solicitacao_servico_servico FOREIGN KEY (servico_id) REFERENCES servico (id);
