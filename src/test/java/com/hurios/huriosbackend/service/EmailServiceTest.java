package com.hurios.huriosbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para EmailService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - Pruebas Unitarias")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ValidationService validationService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Configurar propiedades con ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "from", "test@hurios.com");
        ReflectionTestUtils.setField(emailService, "mockEmail", true); // Modo mock para pruebas
    }

    // ==================== PRUEBAS DE ENVÍO BÁSICO ====================

    @Test
    @DisplayName("Debe enviar email HTML en modo mock")
    void testSendHtml_MockMode() throws Exception {
        // ACT
        assertDoesNotThrow(() -> 
            emailService.sendHtml("test@example.com", "Test Subject", "<p>Test Body</p>")
        );

        // ASSERT - En modo mock no se debe enviar el email real
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ==================== PRUEBAS DE CONFIRMACIÓN DE COMPRA ====================

    @Test
    @DisplayName("Debe enviar confirmación de compra correctamente")
    void testSendPurchaseConfirmation_Success() throws Exception {
        // ARRANGE
        doNothing().when(validationService).validateEmail("test@example.com");
        doNothing().when(validationService).validateId(1L);
        doNothing().when(validationService).validatePrice(100.0);

        // ACT
        assertDoesNotThrow(() -> 
            emailService.sendPurchaseConfirmation("test@example.com", 1L, 100.0)
        );

        // ASSERT
        verify(validationService, times(1)).validateEmail("test@example.com");
        verify(validationService, times(1)).validateId(1L);
        verify(validationService, times(1)).validatePrice(100.0);
    }

    @Test
    @DisplayName("Debe fallar con email inválido en confirmación de compra")
    void testSendPurchaseConfirmation_InvalidEmail() {
        // ARRANGE
        doThrow(new IllegalArgumentException("Email inválido"))
            .when(validationService).validateEmail("invalid");

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendPurchaseConfirmation("invalid", 1L, 100.0)
        );
    }

    @Test
    @DisplayName("Debe fallar con ID de orden inválido")
    void testSendPurchaseConfirmation_InvalidOrderId() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("test@example.com");
        doThrow(new IllegalArgumentException("ID inválido"))
            .when(validationService).validateId(0L);

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendPurchaseConfirmation("test@example.com", 0L, 100.0)
        );
    }

    @Test
    @DisplayName("Debe fallar con total inválido")
    void testSendPurchaseConfirmation_InvalidTotal() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("test@example.com");
        doNothing().when(validationService).validateId(1L);
        doThrow(new IllegalArgumentException("Precio inválido"))
            .when(validationService).validatePrice(-10.0);

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendPurchaseConfirmation("test@example.com", 1L, -10.0)
        );
    }

    // ==================== PRUEBAS DE ACTUALIZACIÓN DE ESTADO ====================

    @Test
    @DisplayName("Debe enviar actualización de estado correctamente")
    void testSendOrderStatusUpdate_Success() throws Exception {
        // ARRANGE
        doNothing().when(validationService).validateEmail("test@example.com");
        doNothing().when(validationService).validateId(1L);

        // ACT
        assertDoesNotThrow(() -> 
            emailService.sendOrderStatusUpdate("test@example.com", 1L, "ENVIADO")
        );

        // ASSERT
        verify(validationService, times(1)).validateEmail("test@example.com");
        verify(validationService, times(1)).validateId(1L);
    }

    @Test
    @DisplayName("Debe fallar con estado vacío")
    void testSendOrderStatusUpdate_EmptyStatus() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("test@example.com");
        doNothing().when(validationService).validateId(1L);

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendOrderStatusUpdate("test@example.com", 1L, "")
        );
    }

    @Test
    @DisplayName("Debe fallar con estado nulo")
    void testSendOrderStatusUpdate_NullStatus() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("test@example.com");
        doNothing().when(validationService).validateId(1L);

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendOrderStatusUpdate("test@example.com", 1L, null)
        );
    }

    // ==================== PRUEBAS DE ALERTAS DE STOCK ====================

    @Test
    @DisplayName("Debe enviar alerta de stock bajo correctamente")
    void testSendLowStockAlert_Success() throws Exception {
        // ARRANGE
        doNothing().when(validationService).validateEmail("admin@hurios.com");

        // ACT
        assertDoesNotThrow(() -> 
            emailService.sendLowStockAlert("admin@hurios.com", "Producto A", 5)
        );

        // ASSERT
        verify(validationService, times(1)).validateEmail("admin@hurios.com");
    }

    @Test
    @DisplayName("Debe fallar con nombre de producto vacío")
    void testSendLowStockAlert_EmptyProductName() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("admin@hurios.com");

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendLowStockAlert("admin@hurios.com", "", 5)
        );
    }

    @Test
    @DisplayName("Debe fallar con stock negativo")
    void testSendLowStockAlert_NegativeStock() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("admin@hurios.com");

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendLowStockAlert("admin@hurios.com", "Producto A", -1)
        );
    }

    @Test
    @DisplayName("Debe fallar con stock nulo")
    void testSendLowStockAlert_NullStock() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("admin@hurios.com");

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendLowStockAlert("admin@hurios.com", "Producto A", null)
        );
    }

    @Test
    @DisplayName("Debe enviar alerta de producto sin stock correctamente")
    void testSendOutOfStockAlert_Success() throws Exception {
        // ARRANGE
        doNothing().when(validationService).validateEmail("admin@hurios.com");

        // ACT
        assertDoesNotThrow(() -> 
            emailService.sendOutOfStockAlert("admin@hurios.com", "Producto B")
        );

        // ASSERT
        verify(validationService, times(1)).validateEmail("admin@hurios.com");
    }

    @Test
    @DisplayName("Debe fallar con nombre de producto nulo en alerta sin stock")
    void testSendOutOfStockAlert_NullProductName() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("admin@hurios.com");

        // ACT & ASSERT
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendOutOfStockAlert("admin@hurios.com", null)
        );
    }

    // ==================== PRUEBAS DE VALIDACIÓN ====================

    @Test
    @DisplayName("Debe validar email correctamente")
    void testIsValidEmail_Valid() {
        // ARRANGE
        doNothing().when(validationService).validateEmail("test@example.com");

        // ACT
        boolean result = emailService.isValidEmail("test@example.com");

        // ASSERT
        assertTrue(result);
    }

    @Test
    @DisplayName("Debe retornar false para email inválido")
    void testIsValidEmail_Invalid() {
        // ARRANGE
        doThrow(new IllegalArgumentException("Email inválido"))
            .when(validationService).validateEmail("invalid-email");

        // ACT
        boolean result = emailService.isValidEmail("invalid-email");

        // ASSERT
        assertFalse(result);
    }

    // ==================== PRUEBAS DE MODO MOCK ====================

    @Test
    @DisplayName("Debe retornar true cuando está en modo mock")
    void testIsMockMode_True() {
        // ACT
        boolean result = emailService.isMockMode();

        // ASSERT
        assertTrue(result);
    }

    @Test
    @DisplayName("Debe retornar false cuando no está en modo mock")
    void testIsMockMode_False() {
        // ARRANGE
        ReflectionTestUtils.setField(emailService, "mockEmail", false);

        // ACT
        boolean result = emailService.isMockMode();

        // ASSERT
        assertFalse(result);
    }

    // ==================== PRUEBAS DE ESCENARIOS MÚLTIPLES ====================

    @Test
    @DisplayName("Debe procesar múltiples emails en secuencia")
    void testMultipleEmails_Success() throws Exception {
        // ARRANGE
        doNothing().when(validationService).validateEmail(anyString());
        doNothing().when(validationService).validateId(anyLong());
        doNothing().when(validationService).validatePrice(anyDouble());

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            emailService.sendPurchaseConfirmation("user1@test.com", 1L, 100.0);
            emailService.sendOrderStatusUpdate("user2@test.com", 2L, "ENVIADO");
            emailService.sendLowStockAlert("admin@test.com", "Producto X", 3);
        });

        verify(validationService, times(3)).validateEmail(anyString());
    }

    @Test
    @DisplayName("Debe manejar caracteres especiales en nombres de productos")
    void testSpecialCharacters_InProductName() throws Exception {
        // ARRANGE
        String productName = "Producto & Artículo Ñoño <Test>";
        doNothing().when(validationService).validateEmail("admin@test.com");

        // ACT
        assertDoesNotThrow(() -> 
            emailService.sendLowStockAlert("admin@test.com", productName, 5)
        );
    }
}
