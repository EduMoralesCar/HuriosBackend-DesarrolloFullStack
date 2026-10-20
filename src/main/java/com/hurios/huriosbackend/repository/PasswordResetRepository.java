package com.hurios.huriosbackend.repository;

import com.hurios.huriosbackend.entity.PasswordReset;
import com.hurios.huriosbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/*
 * PasswordResetRepository:
 * - Permite buscar el token de reset más reciente, válido y no usado
 */
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    // Buscar token válido y no usado para un usuario
    Optional<PasswordReset> findTopByUserAndTokenAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(User user, String token, LocalDateTime now);
}
