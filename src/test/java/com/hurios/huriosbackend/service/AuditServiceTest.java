package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurios.huriosbackend.entity.AuditLog;
import com.hurios.huriosbackend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AuditService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService - Pruebas Unitarias")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("logAction debe guardar un AuditLog con detalles en JSON")
    void testLogAction_Success() throws Exception {
        // ARRANGE
        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        when(objectMapper.writeValueAsString(details)).thenReturn("{\"key\":\"value\"}");

        // ACT
        auditService.logAction(
                "TEST_ACTION",
                "TEST_ENTITY",
                1L,
                2L,
                "user@example.com",
                "127.0.0.1",
                "JUnit",
                details
        );

        // ASSERT
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals("TEST_ACTION", saved.getAction());
        assertEquals("TEST_ENTITY", saved.getEntity());
        assertEquals(1L, saved.getEntityId());
        assertEquals(2L, saved.getUserId());
        assertEquals("user@example.com", saved.getUserEmail());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("JUnit", saved.getUserAgent());
        assertEquals("{\"key\":\"value\"}", saved.getDetails());
        assertEquals("SUCCESS", saved.getStatus());
    }

    @Test
    @DisplayName("logFailure debe guardar un AuditLog con estado FAILURE y mensaje de error")
    void testLogFailure_SetsFailureStatusAndErrorMessage() {
        // ARRANGE
        when(request.getHeader("User-Agent")).thenReturn("JUnit-Agent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        // ACT
        auditService.logFailure(
                "ACTION",
                "ENTITY",
                3L,
                4L,
                "user@example.com",
                request,
                "Something went wrong"
        );

        // ASSERT
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals("FAILURE", saved.getStatus());
        assertEquals("Something went wrong", saved.getErrorMessage());
        assertEquals("10.0.0.1", saved.getIpAddress());
        assertEquals("JUnit-Agent", saved.getUserAgent());
    }

    @Test
    @DisplayName("logLogin debe delegar a logAction y guardar un registro")
    void testLogLogin_DelegatesToLogAction() {
        // ARRANGE
        when(request.getHeader("User-Agent")).thenReturn("Browser");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1");

        // ACT
        auditService.logLogin(1L, "user@example.com", request);

        // ASSERT
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("getUserLogs debe delegar en el repositorio")
    void testGetUserLogs_DelegatesToRepository() {
        // ARRANGE
        List<AuditLog> logs = List.of(new AuditLog());
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(logs);

        // ACT
        List<AuditLog> result = auditService.getUserLogs(1L);

        // ASSERT
        assertEquals(1, result.size());
        verify(auditLogRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("getLogsInDateRange debe delegar en el repositorio")
    void testGetLogsInDateRange_DelegatesToRepository() {
        // ARRANGE
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<AuditLog> logs = List.of(new AuditLog());
        when(auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)).thenReturn(logs);

        // ACT
        List<AuditLog> result = auditService.getLogsInDateRange(start, end);

        // ASSERT
        assertEquals(1, result.size());
        verify(auditLogRepository, times(1)).findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    @Test
    @DisplayName("getActionStatistics debe construir mapa a partir de resultados crudos")
    void testGetActionStatistics_BuildsMap() {
        // ARRANGE
        Object[] row1 = new Object[]{"LOGIN", 5L};
        Object[] row2 = new Object[]{"LOGOUT", 2L};
        when(auditLogRepository.getActionStatistics()).thenReturn(List.of(row1, row2));

        // ACT
        Map<String, Long> stats = auditService.getActionStatistics();

        // ASSERT
        assertEquals(2, stats.size());
        assertEquals(5L, stats.get("LOGIN"));
        assertEquals(2L, stats.get("LOGOUT"));
    }

    @Test
    @DisplayName("getFailedActions debe delegar en el repositorio")
    void testGetFailedActions_DelegatesToRepository() {
        // ARRANGE
        List<AuditLog> logs = List.of(new AuditLog());
        when(auditLogRepository.findFailedActions()).thenReturn(logs);

        // ACT
        List<AuditLog> result = auditService.getFailedActions();

        // ASSERT
        assertEquals(1, result.size());
        verify(auditLogRepository, times(1)).findFailedActions();
    }
}
