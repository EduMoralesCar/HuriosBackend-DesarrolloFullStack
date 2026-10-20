package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurios.huriosbackend.document.AuditLog;
import com.hurios.huriosbackend.repository.mongo.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Registra una acción de auditoría de forma asíncrona en MongoDB
     */
    @Async
    public void logAction(String action, String entity, Long entityId, Long userId, String userEmail,
                         String ipAddress, String userAgent, Map<String, Object> details) {
        try {
            AuditLog log = new AuditLog(action, entity, entityId, userId, userEmail);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);

            if (details != null && !details.isEmpty()) {
                log.setDetails(objectMapper.writeValueAsString(details));
            }

            auditLogRepository.save(log);
        } catch (Exception e) {
            // Log error pero no interrumpir el flujo principal
            System.err.println("Error logging audit action: " + e.getMessage());
        }
    }

    /**
     * Registra una acción exitosa
     */
    @Async
    public void logSuccess(String action, String entity, Long entityId, Long userId, String userEmail,
                          HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        logAction(action, entity, entityId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Registra una acción fallida en MongoDB
     */
    @Async
    public void logFailure(String action, String entity, Long entityId, Long userId, String userEmail,
                          HttpServletRequest request, String errorMessage) {
        try {
            AuditLog log = new AuditLog(action, entity, entityId, userId, userEmail);
            log.setIpAddress(getClientIP(request));
            log.setUserAgent(getUserAgent(request));
            log.setStatus("FAILURE");
            log.setErrorMessage(errorMessage);

            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Error logging audit failure: " + e.getMessage());
        }
    }

    /**
     * Registra login exitoso
     */
    public void logLogin(Long userId, String userEmail, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("loginTime", LocalDateTime.now().toString());
        logAction("LOGIN", "USER", userId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Registra logout
     */
    public void logLogout(Long userId, String userEmail, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("logoutTime", LocalDateTime.now().toString());
        logAction("LOGOUT", "USER", userId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Registra registro de nuevo usuario
     */
    public void logUserRegistration(Long userId, String userEmail, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("registrationTime", LocalDateTime.now().toString());
        logAction("REGISTER", "USER", userId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Registra una compra
     */
    public void logPurchase(Long saleId, Long userId, String userEmail, double amount,
                           String paymentMethod, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("amount", amount);
        details.put("paymentMethod", paymentMethod);
        details.put("purchaseTime", LocalDateTime.now().toString());
        logAction("PURCHASE", "SALE", saleId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Registra actualización de producto
     */
    public void logProductUpdate(Long productId, Long userId, String userEmail,
                                String changeType, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("changeType", changeType);
        details.put("updateTime", LocalDateTime.now().toString());
        logAction("UPDATE_PRODUCT", "PRODUCT", productId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Registra creación de producto
     */
    public void logProductCreation(Long productId, Long userId, String userEmail, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("creationTime", LocalDateTime.now().toString());
        logAction("CREATE_PRODUCT", "PRODUCT", productId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Registra eliminación de producto
     */
    public void logProductDeletion(Long productId, Long userId, String userEmail, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("deletionTime", LocalDateTime.now().toString());
        logAction("DELETE_PRODUCT", "PRODUCT", productId, userId, userEmail,
                 getClientIP(request), getUserAgent(request), details);
    }

    /**
     * Obtiene todos los logs ordenados por fecha descendente
     */
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Obtiene logs de un usuario
     */
    public List<AuditLog> getUserLogs(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Obtiene logs por acción
     */
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action);
    }

    /**
     * Obtiene logs por entidad
     */
    public List<AuditLog> getLogsByEntity(String entity) {
        return auditLogRepository.findByEntityOrderByCreatedAtDesc(entity);
    }

    /**
     * Obtiene logs en un rango de fechas
     */
    public List<AuditLog> getLogsInDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    /**
     * Obtiene estadísticas de acciones agrupadas desde MongoDB
     */
    public Map<String, Long> getActionStatistics() {
        List<AuditLog> logs = auditLogRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        for (AuditLog log : logs) {
            if (log.getAction() != null) {
                stats.merge(log.getAction(), 1L, Long::sum);
            }
        }
        return stats;
    }

    /**
     * Obtiene logs de acciones fallidas
     */
    public List<AuditLog> getFailedActions() {
        return auditLogRepository.findFailedActions();
    }

    /**
     * Extrae la IP del cliente
     */
    private String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * Extrae el User-Agent de forma segura
     */
    private String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return request.getHeader("User-Agent");
    }
}
