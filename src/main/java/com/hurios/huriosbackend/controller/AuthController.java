package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.service.AuditService;
import com.hurios.huriosbackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
 AuthController - expone endpoints que usan tu AuthService.
 - CORS habilitado en desarrollo con @CrossOrigin(origins = "*")
 - Cada método llama al AuthService y devuelve respuestas HTTP adecuadas.
*/
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    // POST /auth/register
    // body: { "email": "...", "password": "...", "fullName": "...", "phone": "..." }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = body.get("email");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String phone = body.get("phone");
        String dni = body.get("dni");
        try {
            String msg = authService.register(email, password, fullName, phone, dni);
            if (msg != null && msg.startsWith("Correo ya registrado")) {
                return ResponseEntity.badRequest().body(Map.of("error", msg));
            }
            auditService.logUserRegistration(null, email, request);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            auditService.logFailure("REGISTER", "USER", null, null, email, request, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /auth/login
    // body: { "email": "...", "password": "...", "role": "CLIENTE" o "ADMINISTRADOR" }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = body.get("email");
        String password = body.get("password");
        String role = body.get("role"); // CLIENTE o ADMINISTRADOR
        Map<String, Object> res = authService.login(email, password, role);
        // authService comunica ok:false con message si falla
        boolean ok = Boolean.TRUE.equals(res.get("ok"));
        if (!ok) {
            auditService.logFailure("LOGIN", "USER", null, null, email, request, String.valueOf(res.get("message")));
            // 401 para credenciales inválidas o usuario no encontrado
            return ResponseEntity.status(401).body(res);
        }
        auditService.logLogin(null, email, request);
        return ResponseEntity.ok(res);
    }

    // POST /auth/send-verification-code  (envía código por email)
    // body: { "email": "..." }
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String,String> body) {
        try {
            String email = body.get("email");
            String msg = authService.sendVerificationCode(email);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /auth/verify-code
    // body: { "email": "...", "code": "123456" }
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String,String> body, HttpServletRequest request) {
        String email = body.get("email");
        String code = body.get("code");
        Map<String, Object> res = authService.verifyCodeAndLogin(email, code);
        boolean ok = Boolean.TRUE.equals(res.get("ok"));
        if (ok) {
            auditService.logSuccess("VERIFY_EMAIL", "USER", null, null, email, request);
            return ResponseEntity.ok(res);
        }
        auditService.logFailure("VERIFY_EMAIL", "USER", null, null, email, request, String.valueOf(res.get("message")));
        return ResponseEntity.badRequest().body(Map.of("error", res.get("message")));
    }

    // POST /auth/request-password-reset
    // body: { "email": "..." }
    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String,String> body) {
        try {
            String email = body.get("email");
            Map<String, Object> res = authService.requestPasswordReset(email);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /auth/verify-reset-code
    // body: { "email": "...", "code": "..." }
    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String code = body.get("code");
        boolean valid = authService.verifyResetCode(email, code);
        if (valid) {
            return ResponseEntity.ok(Map.of("ok", true, "message", "Código verificado correctamente"));
        }
        return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Código inválido o expirado"));
    }

    // POST /auth/reset-password
    // body: { "email": "...", "token": "...", "newPassword": "..." }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String,String> body, HttpServletRequest request) {
        String email = body.get("email");
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        String msg = authService.resetPassword(email, token, newPassword);
        if (msg != null && msg.startsWith("Contraseña actualizada")) {
            auditService.logSuccess("RESET_PASSWORD", "USER", null, null, email, request);
            return ResponseEntity.ok(Map.of("message", msg));
        }
        auditService.logFailure("RESET_PASSWORD", "USER", null, null, email, request, msg);
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
