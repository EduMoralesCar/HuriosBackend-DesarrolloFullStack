package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.document.AuditLog;
import com.hurios.huriosbackend.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para consultar la bitácora de auditoría almacenada en MongoDB ("audit_logs").
 */
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditService.getAllLogs());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(auditService.getUserLogs(userId));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getLogsByAction(@PathVariable String action) {
        return ResponseEntity.ok(auditService.getLogsByAction(action.toUpperCase()));
    }

    @GetMapping("/entity/{entity}")
    public ResponseEntity<List<AuditLog>> getLogsByEntity(@PathVariable String entity) {
        return ResponseEntity.ok(auditService.getLogsByEntity(entity.toUpperCase()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getActionStatistics() {
        return ResponseEntity.ok(auditService.getActionStatistics());
    }

    @GetMapping("/failed")
    public ResponseEntity<List<AuditLog>> getFailedActions() {
        return ResponseEntity.ok(auditService.getFailedActions());
    }
}
