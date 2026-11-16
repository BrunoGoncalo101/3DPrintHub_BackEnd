ALTER TABLE pedidos_personalizados
    ADD COLUMN reserved_stock_grams DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE pedidos_personalizados
    ADD CONSTRAINT chk_pedidos_stock_reservado_nao_negativo
        CHECK (reserved_stock_grams >= 0);