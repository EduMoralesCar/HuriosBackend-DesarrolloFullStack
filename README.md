# HuriosRally Backend

Backend en Spring Boot para la plataforma Hurios Rally. El proyecto ya incluye la base funcional para autenticación, gestión de usuarios, productos, ventas, exportaciones, carga de imágenes y respaldos.

## Qué hace

- Autenticación con JWT, registro de usuarios y recuperación de contraseña.
- Gestión de perfil de usuario y carga de imagen de perfil.
- CRUD de productos y actualización de stock.
- Registro y consulta de ventas y órdenes del usuario.
- Exportación de clientes, productos y ventas a Excel.
- Subida de imágenes a almacenamiento local o Supabase Storage.
- Generación de respaldos de configuración.
- Documentación Swagger/OpenAPI y endpoint público de estado.
- Métricas del backend con Actuator.

## Tecnologías y servicios

- Spring Boot 3.5.6
- Java 17
- Spring Security
- Spring Data JPA
- PostgreSQL
- H2 para desarrollo local
- Springdoc OpenAPI / Swagger UI
- JJWT para tokens JWT
- Apache POI para exportación a Excel
- Spring Mail para correos
- Micrometer y Actuator para observabilidad
- Supabase para base de datos y almacenamiento, cuando está configurado

## Funcionalidad expuesta

### Autenticación
- `POST /auth/register`: crea un usuario nuevo.
- `POST /auth/login`: autentica y devuelve acceso.
- `POST /auth/send-verification-code`: envía código de verificación.
- `POST /auth/verify-code`: valida el código y confirma la sesión.
- `POST /auth/request-password-reset`: inicia recuperación de contraseña.
- `POST /auth/reset-password`: restablece la contraseña.

### Usuarios
- `GET /user/profile`: obtiene el perfil autenticado.
- `PUT /user/profile`: actualiza datos del perfil.
- `POST /user/profile-image`: sube la imagen del perfil.

### Productos
- `GET /products`: lista productos.
- `POST /products`: crea un producto.
- `GET /products/{id}`: consulta un producto.
- `PUT /products/{id}`: actualiza un producto.
- `DELETE /products/{id}`: elimina un producto.
- `PUT /products/{id}/add-stock`: incrementa stock.
- `GET /products/search`: busca por nombre.

### Pagos y ventas
- `POST /payments/process`: procesa una compra y registra la venta.
- `GET /payments/{id}`: consulta una venta.
- `GET /payments/my-orders`: lista órdenes del usuario autenticado.
- `GET /payments/all`: lista todas las ventas.

### Carga y exportación
- `POST /api/images/upload`: sube una imagen y devuelve su URL pública.
- `GET /export/clients`: exporta clientes a Excel.
- `GET /export/products`: exporta productos a Excel.
- `GET /export/sales`: exporta ventas a Excel.

### Otros
- `POST /backup/config`: genera un respaldo de archivos de configuración.
- `GET /health-public`: verifica que el servicio está en línea.
- `GET /`: estado básico público.

## Requisitos

- Java 17
- Maven 3.9+ o el Maven Wrapper incluido
- Base de datos disponible si no usas el perfil local con H2
- Opcional: Supabase, Resend o un servidor SMTP según el entorno

## Configuración

El proyecto arranca por defecto con el perfil `local`.

### Variables importantes
- `PORT`: puerto HTTP del servidor, por defecto `8080`.
- `SPRING_PROFILES_ACTIVE`: perfil activo, por defecto `local`.
- `JWT_SECRET`: secreto para firmar tokens.
- `JWT_EXPIRATION_MINUTES`: duración del token en minutos.
- `APP_FRONTEND_URL`: URL del frontend autorizado.
- `APP_CORS_ALLOWED_ORIGINS`: orígenes permitidos por CORS.
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`: configuración SMTP.
- `RESEND_API_KEY`: clave para envío de correos con Resend.
- `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_STORAGE_BUCKET`: configuración de Supabase.
- `BACKUP_BASE_PATH`: ruta base para respaldos.

### Perfil local

Con `application-local.properties` el proyecto usa H2 en archivo local y activa la consola H2 en `/h2-console`.

## Cómo ejecutar

### Opción 1: Desarrollo local con Maven Wrapper

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

### Opción 2: Compilar y ejecutar el JAR

```bash
./mvnw clean package -DskipTests
java -jar target/huriosbackend-0.0.1-SNAPSHOT.jar
```

En Windows:

```bash
mvnw.cmd clean package -DskipTests
java -jar target/huriosbackend-0.0.1-SNAPSHOT.jar
```

### Opción 3: Con Docker

```bash
docker build -t huriosbackend .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod huriosbackend
```

## URLs útiles

- Aplicación: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/documentacion`
- Actuator health: `http://localhost:8080/actuator/health`
- Consola H2: `http://localhost:8080/h2-console`

## Documentación adicional

- `TESTING_README.md`: guía para ejecutar pruebas unitarias.
- `METRICS_USAGE_EXAMPLE.md`: ejemplo de uso de métricas.

## Notas

- El proyecto no incluye credenciales reales.
- Para entorno productivo, configura correctamente la base de datos, el correo saliente y las claves de Supabase antes de desplegar.
