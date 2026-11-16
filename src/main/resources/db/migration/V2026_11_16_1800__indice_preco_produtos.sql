SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_produtos_preco_base ON produtos (preco_base)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'produtos'
      AND index_name = 'idx_produtos_preco_base'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;