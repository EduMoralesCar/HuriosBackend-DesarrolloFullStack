package com.hurios.huriosbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 User: entidad JPA que representa usuarios en la BD.
 - id: PK autogenerada
 - email: único
 - passwordHash: contraseña encriptada (BCrypt)
 - isVerified: (opcional) para verificación por correo
 - createdAt / updatedAt: timestamps
*/
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "role", nullable = false)
    private String role = "CLIENTE"; // CLIENTE o ADMINISTRADOR

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = true; // por ahora true para evitar flow de verificación

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // getters y setters
    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}

    public String getEmail(){return email;}
    public void setEmail(String email){this.email = email;}

    public String getPasswordHash(){return passwordHash;}
    public void setPasswordHash(String passwordHash){this.passwordHash = passwordHash;}

    public boolean isVerified(){return isVerified;}
    public void setVerified(boolean verified){isVerified = verified;}

    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}

    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt = updatedAt;}

    public String getFullName(){return fullName;}
    public void setFullName(String fullName){this.fullName = fullName;}

    public String getPhone(){return phone;}
    public void setPhone(String phone){this.phone = phone;}

    public String getAddress(){return address;}
    public void setAddress(String address){this.address = address;}

    public String getRole(){return role;}
    public void setRole(String role){this.role = role;}

    public String getProfileImage(){return profileImage;}
    public void setProfileImage(String profileImage){this.profileImage = profileImage;}
}
