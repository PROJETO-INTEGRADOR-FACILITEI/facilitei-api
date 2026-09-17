-- DEFAULT 0.00 evita falha em ambientes com linhas já existentes
-- (ALTER TABLE ... ADD COLUMN NOT NULL sem default falha em tabela não vazia).
ALTER TABLE servico
    ADD COLUMN preco DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
