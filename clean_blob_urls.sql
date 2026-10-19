-- Script para limpiar URLs blob: de productos
-- Ejecutar este script en MySQL Workbench o línea de comandos

-- Ver productos con URLs blob:
SELECT id, name, image_url 
FROM products 
WHERE image_url LIKE 'blob:%';

-- Limpiar URLs blob: (establecerlas como NULL)
UPDATE products 
SET image_url = NULL 
WHERE image_url LIKE 'blob:%';

-- Verificar el resultado
SELECT id, name, image_url 
FROM products;
