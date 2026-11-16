CREATE TABLE produto_cores (
                               produto_id INT NOT NULL,
                               cor_id INT NOT NULL,

                               PRIMARY KEY (produto_id, cor_id),

                               CONSTRAINT fk_produto_cores_produto
                                   FOREIGN KEY (produto_id)
                                       REFERENCES produtos(id),

                               CONSTRAINT fk_produto_cores_cor
                                   FOREIGN KEY (cor_id)
                                       REFERENCES cores(id)
);
