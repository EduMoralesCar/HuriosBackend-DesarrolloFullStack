package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.config.JwtUtil;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;

    public UserController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            String email = jwtUtil.validateAndGetSubject(token);

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            Map<String, Object> profile = new HashMap<>();
            profile.put("email", user.getEmail());
            profile.put("fullName", user.getFullName());
            profile.put("phone", user.getPhone());
            profile.put("address", user.getAddress());
            profile.put("role", user.getRole());
            profile.put("createdAt", user.getCreatedAt());
            profile.put("profileImage", user.getProfileImage());

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Token invalido"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> updates) {
        try {
            String token = extractToken(authHeader);
            String email = jwtUtil.validateAndGetSubject(token);

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
            }

            User user = userOpt.get();

            if (updates.containsKey("fullName")) {
                user.setFullName(updates.get("fullName"));
            }
            if (updates.containsKey("phone")) {
                user.setPhone(updates.get("phone"));
            }
            if (updates.containsKey("address")) {
                user.setAddress(updates.get("address"));
            }
            if (updates.containsKey("profileImage")) {
                user.setProfileImage(updates.get("profileImage"));
            }

            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Token invalido"));
        }
    }

    @PostMapping("/profile-image")
    public ResponseEntity<?> uploadProfileImage(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        try {
            String token = extractToken(authHeader);
            String email = jwtUtil.validateAndGetSubject(token);

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
            }

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

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "La imagen no debe superar los 5MB")
                );
            }

            FileStorageService.StoredFile storedFile = fileStorageService.uploadProfileImage(file);

            User user = userOpt.get();
            user.setProfileImage(storedFile.publicUrl());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "imageUrl", storedFile.publicUrl(),
                    "message", "Imagen de perfil actualizada correctamente"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Token invalido"));
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error al guardar la imagen: " + e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error interno del servidor")
            );
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Token invalido");
    }
}
