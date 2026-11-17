CREATE TABLE pedidos_personalizados (
                                        id INT AUTO_INCREMENT PRIMARY KEY,
                                        ficheiro_id INT NOT NULL,
                                        material_id INT NOT NULL,
                                        cor_id INT NOT NULL,
                                        quantidade INT NOT NULL DEFAULT 1,
                                        infill_percent INT NOT NULL DEFAULT 20,
                                        estimated_price DECIMAL(10, 2) NOT NULL,
                                        final_price DECIMAL(10, 2) NULL,
                                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                        rejection_reason VARCHAR(255) NULL,
                                        user_id INT NOT NULL,
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT fk_pedidos_ficheiro FOREIGN KEY (ficheiro_id) REFERENCES ficheiros_modelo(id)
);