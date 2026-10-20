package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.dto.BackupDtos;
import com.hurios.huriosbackend.service.BackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * BackupController - Endpoints para crear backups simples del backend.
 */
@RestController
@RequestMapping("/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * POST /backup/config
     * Crea un backup de archivos de configuración y devuelve la ruta del backup.
     */
    @PostMapping("/config")
    public ResponseEntity<BackupDtos.BackupResponse> createConfigBackup() {
        try {
            String backupPath = backupService.createConfigBackup();
            BackupDtos.BackupResponse response = new BackupDtos.BackupResponse(
                    true,
                    "Backup de configuración creado correctamente.",
                    backupPath
            );
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            BackupDtos.BackupResponse response = new BackupDtos.BackupResponse(
                    false,
                    "Error al crear el backup: " + e.getMessage(),
                    null
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
