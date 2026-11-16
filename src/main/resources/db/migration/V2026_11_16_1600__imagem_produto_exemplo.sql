UPDATE produtos
SET imagem_url = '/images/products/vaso-geometrico.png'
WHERE id = 1
  AND imagem_url IS NULL;