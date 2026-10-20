package com.hurios.huriosbackend.repository;

import com.hurios.huriosbackend.entity.EmailVerificationCode;
import com.hurios.huriosbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/*
 * EmailVerificationCodeRepository:
 * - Método para obtener el código más reciente no expirado para un usuario
 * - Método para eliminar todos los códigos de un usuario (por ejemplo tras verificar)
 */
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    // Buscar el código más reciente y no expirado para un usuario
    Optional<EmailVerificationCode> findTopByUserAndCodeAndExpiresAtAfterOrderByCreatedAtDesc(User user, String code, LocalDateTime now);

    // Eliminar todos los códigos de un usuario
    void deleteByUser(User user);
}
