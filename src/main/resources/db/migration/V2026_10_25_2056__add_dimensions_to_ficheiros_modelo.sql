ALTER TABLE ficheiros_modelo
    ADD COLUMN dimensao_x DECIMAL(10, 2) NULL AFTER volume_cm3,
    ADD COLUMN dimensao_y DECIMAL(10, 2) NULL AFTER dimensao_x,
    ADD COLUMN dimensao_z DECIMAL(10, 2) NULL AFTER dimensao_y;