package com.hurios.huriosbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BackupService - Servicio sencillo para crear backups de archivos clave de configuración.
 */
@Service
public class BackupService {

    /**
     * Carpeta base donde se guardarán los backups.
     * Por defecto: "uploads/backups".
     */
    private final String backupBasePath;

    public BackupService(@Value("${backup.base-path:uploads/backups}") String backupBasePath) {
        this.backupBasePath = backupBasePath;
    }

    /**
     * Crea un backup de archivos de configuración principales del backend.
     * - application.properties
     * - scripts de migración en db/migration
     *
     * @return ruta del directorio de backup creado
     * @throws IOException si ocurre un error de E/S
     */
    public String createConfigBackup() throws IOException {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        Path backupDir = Paths.get(backupBasePath, timestamp);

        Files.createDirectories(backupDir);

        // Copiar application.properties
        copyResourceToDirectory("application.properties", backupDir);

        // Copiar scripts de migración (si existen)
        copyResourceToDirectory("db/migration/add_profile_image.sql", backupDir.resolve("db/migration"));

        return backupDir.toAbsolutePath().toString();
    }

    private void copyResourceToDirectory(String resourcePath, Path targetDir) throws IOException {
        Resource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            return; // Nada que copiar
        }

        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(extractFileName(resourcePath));

        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, targetFile);
        }
    }

    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }
}
