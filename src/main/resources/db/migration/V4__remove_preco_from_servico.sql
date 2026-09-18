SET @drop_preco = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE servico DROP COLUMN preco',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'servico'
      AND column_name = 'preco'
);

PREPARE drop_preco_statement FROM @drop_preco;
EXECUTE drop_preco_statement;
DEALLOCATE PREPARE drop_preco_statement;
