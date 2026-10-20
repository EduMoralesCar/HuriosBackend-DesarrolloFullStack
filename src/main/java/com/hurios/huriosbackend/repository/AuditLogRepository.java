package com.hurios.huriosbackend.repository;

import com.hurios.huriosbackend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
    
    List<AuditLog> findByEntityOrderByCreatedAtDesc(String entity);
    
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime start, 
        LocalDateTime end
    );
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<AuditLog> findUserActivityInDateRange(
        @Param("userId") Long userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> getActionStatistics();
    
    @Query("SELECT a FROM AuditLog a WHERE a.status = 'FAILURE' OR a.status = 'ERROR' ORDER BY a.createdAt DESC")
    List<AuditLog> findFailedActions();
}
