-- =============================================================================
-- HURIOS RALLY E.I.R.L. - SCRIPT DE CREACIÓN E INICIALIZACIÓN (DDL + DML)
-- Base de Datos Relacional: PostgreSQL (Supabase) / H2
-- =============================================================================

-- 1. TABLA DE USUARIOS (users)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'CLIENTE',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_code VARCHAR(20),
    verification_expiry TIMESTAMP,
    reset_token VARCHAR(255),
    reset_expiry TIMESTAMP,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(500),
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 2. TABLA DE PRODUCTOS / REPUESTOS (products)
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE PRECISION NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image_url VARCHAR(500),
    category VARCHAR(120),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);

-- 3. TABLA DE VENTAS / ÓRDENES (sales)
CREATE TABLE IF NOT EXISTS sales (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    dni VARCHAR(20),
    company_name VARCHAR(255),
    ruc VARCHAR(20),
    company_address VARCHAR(500),
    delivery_method VARCHAR(50) NOT NULL,
    delivery_address VARCHAR(500),
    delivery_district VARCHAR(120),
    delivery_reference VARCHAR(500),
    payment_method VARCHAR(50) NOT NULL,
    payment_details TEXT,
    subtotal DOUBLE PRECISION NOT NULL,
    shipping_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sales_user_id ON sales(user_id);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status);
CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at);

-- 4. TABLA DE ÍTEMS DE VENTA (sale_items)
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DOUBLE PRECISION NOT NULL CHECK (unit_price >= 0),
    subtotal DOUBLE PRECISION NOT NULL CHECK (subtotal >= 0)
);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id);

-- 5. TABLA DE TOKENS DE VERIFICACIÓN (verification_tokens)
CREATE TABLE IF NOT EXISTS verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL
);

-- =============================================================================
-- DATOS SEMILLA INICIALES (DML)
-- =============================================================================
INSERT INTO products (name, description, price, stock, image_url, category)
SELECT 'Kit de Embrague Reforzado Racing', 'Kit completo de disco, plato opresor y collarín de alto desempeño para vehículos todo terreno y competencia.', 485.00, 15, '/assets/imgs/producto1.webp', 'Transmisión'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Kit de Embrague Reforzado Racing');

INSERT INTO products (name, description, price, stock, image_url, category)
SELECT 'Amortiguador Hidráulico Rally Pro', 'Amortiguador de gas nitrógeno de doble tubo con regulación de rebote para caminos exigentes.', 320.00, 24, '/assets/imgs/producto1.webp', 'Suspensión'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Amortiguador Hidráulico Rally Pro');

INSERT INTO products (name, description, price, stock, image_url, category)
SELECT 'Pastillas de Freno Cerámicas Brembo', 'Juego de 4 pastillas cerámicas de alta fricción y baja emisión de polvo para frenado extremo.', 165.50, 40, '/assets/imgs/producto1.webp', 'Frenos'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Pastillas de Freno Cerámicas Brembo');

INSERT INTO products (name, description, price, stock, image_url, category)
SELECT 'Filtro de Aire de Alto Flujo K&N', 'Filtro lavable y reutilizable que incrementa el flujo de aire y la potencia del motor.', 140.00, 30, '/assets/imgs/producto1.webp', 'Motor'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Filtro de Aire de Alto Flujo K&N');
