ALTER TABLE ficheiros_modelo
    ADD COLUMN eliminado_em DATETIME NULL;

ALTER TABLE pedidos_personalizados
    ADD COLUMN finished_at DATETIME NULL;

UPDATE pedidos_personalizados
SET finished_at = created_at
WHERE status IN ('APPROVED', 'REJECTED')
  AND finished_at IS NULL;

CREATE INDEX idx_ficheiros_modelo_eliminado_em ON ficheiros_modelo (eliminado_em);
CREATE INDEX idx_pedidos_personalizados_status_finished ON pedidos_personalizados (status, finished_at);