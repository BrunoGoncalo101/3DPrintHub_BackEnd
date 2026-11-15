CREATE TABLE historico_encomendas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    encomenda_id    INT NOT NULL,
    estado_anterior VARCHAR(30) NULL,
    estado_novo     VARCHAR(30) NOT NULL,
    alterado_por    INT NULL,
    alterado_em     DATETIME(6) NOT NULL,
    CONSTRAINT fk_historico_encomendas   FOREIGN KEY (encomenda_id) REFERENCES encomendas(id),
    CONSTRAINT fk_historico_utilizadores FOREIGN KEY (alterado_por) REFERENCES utilizadores(id)
);