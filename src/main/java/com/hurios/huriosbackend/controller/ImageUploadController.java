package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {

    private final FileStorageService fileStorageService;

    public ImageUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "El archivo esta vacio")
                );
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "El archivo debe ser una imagen")
                );
            }

            FileStorageService.StoredFile storedFile = fileStorageService.uploadProductImage(file);

            return ResponseEntity.ok(Map.of(
                    "imageUrl", storedFile.publicUrl(),
                    "filename", storedFile.filename(),
                    "message", "Imagen subida correctamente"
            ));

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error al subir la imagen: " + e.getMessage())
            );
        }
    }
}
