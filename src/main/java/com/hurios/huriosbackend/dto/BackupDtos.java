package com.hurios.huriosbackend.dto;

/**
 * DTOs relacionados a operaciones de backup.
 */
public class BackupDtos {

    /**
     * Respuesta simple para la creación de un backup.
     */
    public static class BackupResponse {
        public boolean success;
        public String message;
        public String backupPath;

        public BackupResponse(boolean success, String message, String backupPath) {
            this.success = success;
            this.message = message;
            this.backupPath = backupPath;
        }
    }
}
