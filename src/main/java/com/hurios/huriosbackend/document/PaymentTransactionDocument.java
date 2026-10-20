package com.hurios.huriosbackend.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Documento MongoDB que almacena las transacciones, intentos y respuestas completas
 * de la pasarela de pagos (MercadoPago Checkout Pro) en la colección "payment_transactions".
 */
@Document(collection = "payment_transactions")
public class PaymentTransactionDocument {

    @Id
    private String id;

    @Indexed
    private Long saleId;

    @Indexed
    private Long userId;

    @Indexed
    private String userEmail;

    @Indexed
    private String preferenceId;

    @Indexed
    private String mpPaymentId;

    private String paymentMethod;

    @Indexed
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    private Double amount;

    private String currency = "PEN";

    private List<PaymentAttempt> attempts = new ArrayList<>();

    private Map<String, Object> gatewayResponse = new HashMap<>();

    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public PaymentTransactionDocument() {
    }

    public PaymentTransactionDocument(Long saleId, Long userId, String userEmail, Double amount, String paymentMethod) {
        this.saleId = saleId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = "PENDING";
        this.currency = "PEN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addAttempt(String status, String mpPaymentId, String detail) {
        PaymentAttempt attempt = new PaymentAttempt(status, mpPaymentId, detail, LocalDateTime.now());
        this.attempts.add(attempt);
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
    }

    public String getMpPaymentId() {
        return mpPaymentId;
    }

    public void setMpPaymentId(String mpPaymentId) {
        this.mpPaymentId = mpPaymentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<PaymentAttempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<PaymentAttempt> attempts) {
        this.attempts = attempts != null ? attempts : new ArrayList<>();
    }

    public Map<String, Object> getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(Map<String, Object> gatewayResponse) {
        this.gatewayResponse = gatewayResponse != null ? gatewayResponse : new HashMap<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Subdocumento que registra cada intento o evento de notificación de pago.
     */
    public static class PaymentAttempt {
        private String status;
        private String mpPaymentId;
        private String detail;
        private LocalDateTime timestamp;

        public PaymentAttempt() {
        }

        public PaymentAttempt(String status, String mpPaymentId, String detail, LocalDateTime timestamp) {
            this.status = status;
            this.mpPaymentId = mpPaymentId;
            this.detail = detail;
            this.timestamp = timestamp;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMpPaymentId() {
            return mpPaymentId;
        }

        public void setMpPaymentId(String mpPaymentId) {
            this.mpPaymentId = mpPaymentId;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
