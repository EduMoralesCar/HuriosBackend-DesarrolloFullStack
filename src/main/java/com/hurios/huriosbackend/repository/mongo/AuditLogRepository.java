package com.hurios.huriosbackend.repository.mongo;

import com.hurios.huriosbackend.document.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repositorio MongoDB para la colección "audit_logs".
 */
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    List<AuditLog> findByEntityOrderByCreatedAtDesc(String entity);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    List<AuditLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<AuditLog> findByStatusInOrderByCreatedAtDesc(Collection<String> statuses);

    default List<AuditLog> findFailedActions() {
        return findByStatusInOrderByCreatedAtDesc(List.of("FAILURE", "ERROR"));
    }
}
