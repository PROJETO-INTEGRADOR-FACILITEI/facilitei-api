-- Lista de profissões em tabela (antes era um enum fixo no código).
-- As 28 da plataforma são inseridas pela própria API ao iniciar (ProfissaoService);
-- as personalizadas são criadas pelos profissionais no cadastro.
-- As outras tabelas continuam guardando o código em texto (ex.: PEDREIRO), então nada muda nelas.
CREATE TABLE profissao (
    codigo VARCHAR(60) NOT NULL,
    nome VARCHAR(80) NOT NULL,
    personalizada BOOLEAN NOT NULL DEFAULT FALSE,
    ordem INT NOT NULL DEFAULT 0,
    criada_por_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (codigo)
);

