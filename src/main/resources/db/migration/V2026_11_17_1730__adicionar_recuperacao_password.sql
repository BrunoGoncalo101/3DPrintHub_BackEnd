ALTER TABLE utilizadores
    ADD COLUMN reset_token_hash      VARCHAR(64) NULL,
    ADD COLUMN reset_token_expira_em DATETIME(6) NULL,
    ADD CONSTRAINT uq_utilizadores_reset_token UNIQUE (reset_token_hash);