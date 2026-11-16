ALTER TABLE cores
    ADD COLUMN stock_gramas DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE cores
    ADD CONSTRAINT chk_cores_stock_nao_negativo
        CHECK (stock_gramas >= 0);