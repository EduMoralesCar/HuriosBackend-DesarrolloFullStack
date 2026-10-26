/**
 * =============================================================================
 * HURIOS RALLY E.I.R.L. - SCRIPT DE INICIALIZACIÓN DE COLECCIONES NOSQL
 * Base de Datos Documental: MongoDB Atlas (Base de datos: hurios_rally)
 * =============================================================================
 */

const dbName = "hurios_rally";
const dbRef = db.getSiblingDB(dbName);

// 1. Colección: audit_logs (Bitácora de auditoría y eventos de seguridad)
dbRef.createCollection("audit_logs");
dbRef.audit_logs.createIndex({ action: 1 });
dbRef.audit_logs.createIndex({ entityType: 1 });
dbRef.audit_logs.createIndex({ userId: 1 });
dbRef.audit_logs.createIndex({ createdAt: -1 });

// 2. Colección: carts (Carritos de compra persistentes por usuario)
dbRef.createCollection("carts");
dbRef.carts.createIndex({ userEmail: 1 }, { unique: true });
dbRef.carts.createIndex({ userId: 1 });

// 3. Colección: payment_transactions (Transacciones e historial de pasarela MercadoPago)
dbRef.createCollection("payment_transactions");
dbRef.payment_transactions.createIndex({ saleId: 1 });
dbRef.payment_transactions.createIndex({ preferenceId: 1 });
dbRef.payment_transactions.createIndex({ mpPaymentId: 1 });
dbRef.payment_transactions.createIndex({ userEmail: 1 });
dbRef.payment_transactions.createIndex({ status: 1 });

// 4. Colección: product_reviews (Reseñas y calificaciones de repuestos)
dbRef.createCollection("product_reviews");
dbRef.product_reviews.createIndex({ productId: 1, createdAt: -1 });
dbRef.product_reviews.createIndex({ productId: 1, userEmail: 1 });

print("Colecciones e índices de MongoDB (hurios_rally) inicializados correctamente.");
