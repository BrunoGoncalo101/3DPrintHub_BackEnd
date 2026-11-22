CREATE TABLE notificacoes (
                              id INT AUTO_INCREMENT PRIMARY KEY,
                              utilizador_id INT NOT NULL,
                              titulo VARCHAR(150) NOT NULL,
                              mensagem TEXT NOT NULL,
                              tipo VARCHAR(40) NOT NULL,
                              referencia_id INT NULL,
                              lida BOOLEAN NOT NULL DEFAULT FALSE,
                              criada_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_notificacoes_utilizador
                                  FOREIGN KEY (utilizador_id) REFERENCES utilizadores(id)
                                      ON DELETE CASCADE
);

CREATE INDEX idx_notificacoes_utilizador_lida ON notificacoes (utilizador_id, lida);