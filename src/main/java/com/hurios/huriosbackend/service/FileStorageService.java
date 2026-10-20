package com.hurios.huriosbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final HttpClient httpClient;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Value("${supabase.storage.bucket:hurios-media}")
    private String supabaseStorageBucket;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public FileStorageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public StoredFile uploadProductImage(MultipartFile file) throws IOException, InterruptedException {
        return upload(file, "products", "uploads/products");
    }

    public StoredFile uploadProfileImage(MultipartFile file) throws IOException, InterruptedException {
        return upload(file, "profiles", "uploads/profiles");
    }

    private StoredFile upload(MultipartFile file, String supabaseFolder, String localFolder)
            throws IOException, InterruptedException {
        String uniqueFilename = buildUniqueFilename(file.getOriginalFilename());

        if (isSupabaseStorageConfigured()) {
            return uploadToSupabase(file, supabaseFolder + "/" + uniqueFilename, uniqueFilename);
        }

        if (isProdProfile()) {
            throw new IOException("Supabase Storage no configurado en producción. Configura SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY y SUPABASE_STORAGE_BUCKET.");
        }

        logger.warn("Supabase Storage no configurado. Se usa almacenamiento local para {}", uniqueFilename);
        return uploadLocally(file, localFolder, uniqueFilename);
    }

    private StoredFile uploadToSupabase(MultipartFile file, String objectPath, String filename)
            throws IOException, InterruptedException {
        String normalizedBaseUrl = supabaseUrl.endsWith("/")
                ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                : supabaseUrl;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBaseUrl + "/storage/v1/object/" + supabaseStorageBucket + "/" + objectPath))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + supabaseServiceRoleKey.trim())
                .header("apikey", supabaseServiceRoleKey.trim())
                .header("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase Storage rechazó la subida: " + response.statusCode() + " - " + response.body());
        }

        String publicUrl = normalizedBaseUrl + "/storage/v1/object/public/" + supabaseStorageBucket + "/" + objectPath;
        return new StoredFile(publicUrl, filename);
    }

    private StoredFile uploadLocally(MultipartFile file, String uploadDir, String filename) throws IOException {
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        Path destinationPath = Paths.get(uploadDir, filename);
        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        String publicPath = "/" + uploadDir.replace("\\", "/") + "/" + filename;
        return new StoredFile(publicPath.replace("//", "/"), filename);
    }

    private boolean isSupabaseStorageConfigured() {
        return supabaseUrl != null && !supabaseUrl.trim().isEmpty()
                && supabaseServiceRoleKey != null && !supabaseServiceRoleKey.trim().isEmpty()
                && supabaseStorageBucket != null && !supabaseStorageBucket.trim().isEmpty();
    }

    private boolean isProdProfile() {
        return activeProfiles != null && activeProfiles.toLowerCase().contains("prod");
    }

    private String buildUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    public record StoredFile(String publicUrl, String filename) {
    }
}
