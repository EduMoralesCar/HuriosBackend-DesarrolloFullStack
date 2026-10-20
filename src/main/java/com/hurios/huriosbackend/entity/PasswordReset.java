package com.hurios.huriosbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * Entidad PasswordReset:
 * - Guarda tokens de reinicio de contraseña vinculados a un usuario
 * - Token puede ser UUID; expiresAt controla validez
 * - Campo 'used' permite invalidar token tras su consumo
 */
@Entity
@Table(name = "password_resets")
public class PasswordReset {

    // Primary key autogenerado
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación many-to-one con User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Token de reset (puede ser UUID)
    @Column(nullable = false, length = 255)
    private String token;

    // Fecha/hora de expiración del token
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Indica si el token ya fue usado
    @Column(nullable = false)
    private boolean used = false;

    // Fecha de creación
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---------- Constructor ----------
    public PasswordReset() { }

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

    // token
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // expiresAt
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // used
    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    // createdAt
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
