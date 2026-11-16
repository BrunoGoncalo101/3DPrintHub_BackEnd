ALTER TABLE encomendas MODIFY utilizador_id INT NULL;

ALTER TABLE encomendas
    ADD COLUMN email_convidado     VARCHAR(150) NULL,
    ADD COLUMN telemovel_convidado VARCHAR(20)  NULL;