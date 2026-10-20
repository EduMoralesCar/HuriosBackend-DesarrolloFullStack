package com.hurios.huriosbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * Entidad EmailVerificationCode:
 * - Guarda códigos de verificación enviados por email
 * - Cada código está vinculado a un User (ManyToOne)
 * - Tiene expiración y contador de intentos
 */
@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCode {

    // Primary key autogenerado
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación many-to-one con User (foreign key user_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Código enviado (ej. "123456")
    @Column(nullable = false, length = 20)
    private String code;

    // Fecha/hora en la que el código expira
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Contador de intentos (opcional para bloqueo tras X intentos)
    @Column(nullable = false)
    private int attempts = 0;

    // Fecha de creación para ordenar/consultas
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---------- Constructor ----------
    public EmailVerificationCode() { }

    // ---------- Getters y Setters ----------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // user
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // code
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    // expiresAt
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // attempts
    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    // createdAt
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
