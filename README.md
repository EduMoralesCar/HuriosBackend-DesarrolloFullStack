# Hurios Rally - Backend API & Real-Time Server

Backend empresarial en **Spring Boot 3.5.6 (Java 17)** para el e-commerce de repuestos automotrices **Hurios Rally E.I.R.L.** Implementa una arquitectura híbrida de persistencia (**PostgreSQL** en Supabase + **MongoDB Atlas**), pasarela de pagos real con **MercadoPago Checkout Pro**, seguridad **JWT + RBAC**, y comunicación bidireccional en tiempo real mediante **WebSockets**.

- **URL Desplegada (Render):** `https://huriosbackend-desarrollofullstack.onrender.com`
- **Documentación Swagger / OpenAPI:** `https://huriosbackend-desarrollofullstack.onrender.com/documentacion`

---

## 1. Arquitectura de Datos Híbrida (Relacional + NoSQL)

El sistema emplea **Persistencia Políglota** separando las responsabilidades transaccionales ACID de los documentos flexibles de alta frecuencia:

- **PostgreSQL (Supabase) — Entidades Relacionales (`com.hurios.huriosbackend.entity`):**
  Gestiona usuarios, catálogo maestro de productos/stock, órdenes de venta (`Sale`) y detalle de líneas de orden (`SaleItem`).
- **MongoDB Atlas (`hurios_rally`) — Colecciones Documentales (`com.hurios.huriosbackend.document`):**
  Gestiona la bitácora de auditoría (`audit_logs`), los carritos de compra sincronizados en la nube (`carts`), el historial detallado de intentos y webhooks de la pasarela MercadoPago (`payment_transactions`), y las reseñas/calificaciones de productos (`product_reviews`).

### Diagrama Entidad-Relación (PostgreSQL - Relacional)

```mermaid
erDiagram
    USERS ||--o{ SALES : "realiza"
    USERS ||--o{ VERIFICATION_TOKENS : "posee"
    SALES ||--|{ SALE_ITEMS : "contiene"
    PRODUCTS ||--o{ SALE_ITEMS : "referenciado_en"

    USERS {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR role "CLIENTE | ADMINISTRADOR"
        BOOLEAN verified
        VARCHAR full_name
        VARCHAR phone
        VARCHAR address
        VARCHAR profile_image_url
        TIMESTAMP created_at
    }

    PRODUCTS {
        BIGINT id PK
        VARCHAR name
        TEXT description
        DOUBLE price
        INT stock
        VARCHAR image_url
        VARCHAR category
        TIMESTAMP created_at
    }

    SALES {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR full_name
        VARCHAR document_type "dni | ruc"
        VARCHAR delivery_method "delivery | pickup"
        VARCHAR payment_method "mercadopago | tarjeta | yape"
        DOUBLE subtotal
        DOUBLE shipping_cost
        DOUBLE total
        VARCHAR status "PENDIENTE | CONFIRMADO | RECHAZADO"
        TIMESTAMP created_at
    }

    SALE_ITEMS {
        BIGINT id PK
        BIGINT sale_id FK
        BIGINT product_id FK
        INT quantity
        DOUBLE unit_price
        DOUBLE subtotal
    }

    VERIFICATION_TOKENS {
        BIGINT id PK
        VARCHAR token UK
        BIGINT user_id FK
        TIMESTAMP expires_at
    }
```

### Diagrama de Colecciones NoSQL (MongoDB Atlas - `hurios_rally`)

```mermaid
classDiagram
    class audit_logs {
        +ObjectId _id
        +String action
        +String entityType
        +String entityId
        +Long userId
        +String userEmail
        +String ipAddress
        +Map~String,Object~ details
        +LocalDateTime createdAt
    }

    class carts {
        +ObjectId _id
        +Long userId
        +String userEmail
        +List~CartItemEntry~ items
        +LocalDateTime updatedAt
    }

    class payment_transactions {
        +ObjectId _id
        +Long saleId
        +Long userId
        +String userEmail
        +String preferenceId
        +String mpPaymentId
        +String status
        +String paymentMethod
        +Double amount
        +String currency
        +Map~String,Object~ gatewayResponse
        +List~PaymentAttempt~ attempts
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class product_reviews {
        +ObjectId _id
        +Long productId
        +Long userId
        +String userEmail
        +String userName
        +Integer rating
        +String comment
        +LocalDateTime createdAt
    }
```

Los scripts de inicialización DDL/DML e índices NoSQL se encuentran en:
- `src/main/resources/db/migration/schema_and_seed.sql`
- `src/main/resources/db/migration/mongodb_init.js`

---

## 2. Comunicación en Tiempo Real con WebSockets

El servidor configura un canal WebSocket bidireccional en `/ws/events` mediante `WebSocketConfig` y `RealtimeNotificationHandler`:

- **Endpoint:** `ws://localhost:8080/ws/events` (local) / `wss://huriosbackend-desarrollofullstack.onrender.com/ws/events` (producción)
- **Eventos emitidos en tiempo real (`broadcastEvent`):**
  - `CONNECTED` / `PONG`: Confirmación de conexión activa y conteo de usuarios conectados en vivo (`activeClients`).
  - `NEW_SALE`: Se emite automáticamente al confirmarse una compra o pago en MercadoPago, notificando a los clientes y actualizando el stock en vivo.
  - `STOCK_UPDATED`: Se emite cuando un administrador abastece unidades de un repuesto (`PUT /products/{id}/add-stock`).
  - `PRODUCT_CREATED` / `PRODUCT_UPDATED` / `PRODUCT_DELETED`: Sincroniza instantáneamente el catálogo de repuestos en todas las pantallas abiertas.
  - `NEW_REVIEW`: Notifica en vivo cuando un cliente califica un producto.

---

## 3. Endpoints Principales de la API REST

### Autenticación y Usuarios (`/auth`, `/user`)
- `POST /auth/register`: Registro de usuario con contraseña encriptada (`BCrypt`) y auditoría en MongoDB.
- `POST /auth/login`: Inicio de sesión con generación de JWT y soporte para código de verificación 2FA.
- `POST /auth/request-password-reset` y `POST /auth/reset-password`: Flujo de recuperación de contraseña por correo.
- `GET /user/profile` y `PUT /user/profile`: Consulta y actualización de perfil autenticado.

### Catálogo e Inventario (`/products`)
- `GET /products` y `GET /products/{id}`: Consulta pública del catálogo.
- `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}`: Gestión CRUD de productos con emisión de eventos WebSocket.
- `PUT /products/{id}/add-stock`: Incremento de stock con notificación en tiempo real.

### Carrito Persistente en MongoDB (`/cart`)
- `GET /cart`: Obtiene el carrito del usuario autenticado desde MongoDB.
- `PUT /cart`: Sincroniza el estado del carrito.
- `POST /cart/merge`: Fusiona el carrito local de invitado con el carrito en la nube al iniciar sesión.
- `DELETE /cart`: Vacía el carrito tras completar un pago.

### Pagos y MercadoPago Checkout Pro (`/payments`)
- `POST /payments/checkout`: Crea la orden en estado `PENDIENTE` en PostgreSQL, genera la preferencia en MercadoPago SDK y guarda la transacción en MongoDB (`payment_transactions`).
- `GET /payments/verify`: Verifica el pago al retornar de MercadoPago, confirma la orden, descuenta el stock y emite el evento `NEW_SALE` por WebSocket.
- `POST /payments/webhook`: Receptor de notificaciones IPN/Webhooks de MercadoPago.
- `POST /payments/process`: Procesamiento directo con Tarjeta o Yape.
- `GET /payments/my-orders` y `GET /payments/all`: Historial de órdenes de compra.
- `GET /payments/transactions`: Historial de transacciones desde MongoDB.

### Reseñas y Auditoría en MongoDB (`/products/{id}/reviews`, `/audit`)
- `GET /products/{productId}/reviews` y `POST /products/{productId}/reviews`: Consulta y publicación de reseñas con promedio de estrellas.
- `GET /audit`, `GET /audit/user/{userId}`, `GET /audit/action/{action}`: Consulta de registros de auditoría en MongoDB.

---

## 4. Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto basándote en `.env.example`:

| Variable | Descripción | Ejemplo |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Perfil de ejecución (`local` o `prod`) | `prod` |
| `DB_HOST` | Host de PostgreSQL (Supabase Pooler) | `aws-0-us-east-2.pooler.supabase.com` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos relacional | `postgres` |
| `DB_USER` | Usuario de PostgreSQL | `postgres.<project-ref>` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `********` |
| `MONGODB_URI` | Cadena de conexión a MongoDB Atlas | `mongodb+srv://usuario:pass@cluster0.mongodb.net/hurios_rally` |
| `MONGODB_DATABASE` | Nombre de la base de datos en MongoDB | `hurios_rally` |
| `JWT_SECRET` | Clave secreta para firma de tokens JWT | `clave-secreta-segura-2026` |
| `MERCADOPAGO_ACCESS_TOKEN` | Access Token de MercadoPago Checkout Pro | `APP_USR-...` |
| `MERCADOPAGO_PUBLIC_KEY` | Public Key de MercadoPago | `APP_USR-...` |
| `APP_FRONTEND_URL` | URL del Frontend para redirecciones y CORS | `https://huriosfrontend-desarrollofullstack.onrender.com` |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos separados por coma | `http://localhost:5173,https://huriosfrontend-desarrollofullstack.onrender.com` |

---

## 5. Instrucciones de Ejecución y Pruebas

### Ejecutar en entorno de desarrollo
```bash
./mvnw spring-boot:run
```
En Windows PowerShell:
```powershell
.\mvnw.cmd spring-boot:run
```

### Ejecutar la suite de pruebas unitarias (JUnit 5 + Mockito)
```powershell
.\mvnw.cmd test
```

### Compilar empaquetado de producción (JAR)
```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target/huriosbackend-0.0.1-SNAPSHOT.jar
```
