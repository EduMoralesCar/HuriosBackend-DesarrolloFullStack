package com.hurios.huriosbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para ValidationService
 * 
 * Esta clase muestra ejemplos de:
 * - Pruebas simples sin mocks (porque ValidationService no tiene dependencias)
 * - Pruebas parametrizadas (para probar múltiples valores)
 * - Verificación de excepciones
 */
@DisplayName("ValidationService - Pruebas Unitarias")
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        // No necesitamos mocks porque ValidationService no tiene dependencias
        validationService = new ValidationService();
    }

    // ==================== PRUEBAS DE EMAIL ====================
    
    @Test
    @DisplayName("Debe validar email correcto")
    void testValidateEmail_Valid() {
        // No debe lanzar excepción
        assertDoesNotThrow(() -> 
            validationService.validateEmail("test@example.com")
        );
    }

    @Test
    @DisplayName("Debe fallar con email vacío")
    void testValidateEmail_Empty() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateEmail("")
        );
        
        assertTrue(exception.getMessage().contains("no puede estar vacío"));
    }

    @Test
    @DisplayName("Debe fallar con email nulo")
    void testValidateEmail_Null() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateEmail(null)
        );
    }

    /**
     * @ParameterizedTest: Ejecuta la misma prueba con diferentes valores
     * Útil para probar múltiples casos sin repetir código
     */
    @ParameterizedTest
    @ValueSource(strings = {"invalido", "@sindominino.com", "espacios @email.com"})
    @DisplayName("Debe fallar con emails inválidos")
    void testValidateEmail_InvalidFormat(String invalidEmail) {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateEmail(invalidEmail)
        );
    }

    @Test
    @DisplayName("Debe normalizar email correctamente")
    void testNormalizeEmail() {
        String result = validationService.normalizeEmail("  TEST@EXAMPLE.COM  ");
        assertEquals("test@example.com", result);
    }

    // ==================== PRUEBAS DE PASSWORD ====================
    
    @Test
    @DisplayName("Debe validar contraseña correcta")
    void testValidatePassword_Valid() {
        assertDoesNotThrow(() -> 
            validationService.validatePassword("password123")
        );
    }

    @Test
    @DisplayName("Debe fallar con contraseña corta")
    void testValidatePassword_TooShort() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePassword("123")
        );
        
        assertTrue(exception.getMessage().contains("al menos 8 caracteres"));
    }

    @Test
    @DisplayName("Debe fallar con contraseña nula")
    void testValidatePassword_Null() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePassword(null)
        );
    }

    // ==================== PRUEBAS DE PRECIO ====================
    
    @Test
    @DisplayName("Debe validar precio correcto")
    void testValidatePrice_Valid() {
        assertDoesNotThrow(() -> 
            validationService.validatePrice(100.50)
        );
    }

    @Test
    @DisplayName("Debe fallar con precio nulo")
    void testValidatePrice_Null() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> validationService.validatePrice(null)
        );
        
        assertTrue(exception.getMessage().contains("no puede ser nulo"));
    }

    @Test
    @DisplayName("Debe fallar con precio negativo")
    void testValidatePrice_Negative() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePrice(-10.0)
        );
    }

    @Test
    @DisplayName("Debe fallar con precio cero")
    void testValidatePrice_Zero() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePrice(0.0)
        );
    }

    @Test
    @DisplayName("Debe fallar con precio muy alto")
    void testValidatePrice_TooHigh() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePrice(1000000.0)
        );
    }

    // ==================== PRUEBAS DE STOCK ====================
    
    @Test
    @DisplayName("Debe validar stock correcto")
    void testValidateStock_Valid() {
        assertDoesNotThrow(() -> {
            validationService.validateStock(10);
            validationService.validateStock(0); // Cero es válido
        });
    }

    @Test
    @DisplayName("Debe aceptar stock nulo")
    void testValidateStock_Null() {
        // El método permite null
        assertDoesNotThrow(() -> 
            validationService.validateStock(null)
        );
    }

    @Test
    @DisplayName("Debe fallar con stock negativo")
    void testValidateStock_Negative() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateStock(-5)
        );
    }

    // ==================== PRUEBAS DE NOMBRE DE PRODUCTO ====================
    
    @Test
    @DisplayName("Debe validar nombre de producto correcto")
    void testValidateProductName_Valid() {
        assertDoesNotThrow(() -> 
            validationService.validateProductName("Producto de Prueba")
        );
    }

    @Test
    @DisplayName("Debe fallar con nombre muy corto")
    void testValidateProductName_TooShort() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateProductName("AB")
        );
    }

    @Test
    @DisplayName("Debe fallar con nombre muy largo")
    void testValidateProductName_TooLong() {
        String longName = "A".repeat(101); // 101 caracteres
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateProductName(longName)
        );
    }

    // ==================== PRUEBAS DE TELÉFONO ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"987654321", "912345678"})
    @DisplayName("Debe validar teléfonos válidos")
    void testValidatePhone_Valid(String phone) {
        assertDoesNotThrow(() -> 
            validationService.validatePhone(phone)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "9876543210", "98765432A", "98 765 321"})
    @DisplayName("Debe fallar con teléfonos inválidos")
    void testValidatePhone_Invalid(String phone) {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePhone(phone)
        );
    }

    @Test
    @DisplayName("Debe aceptar teléfono nulo o vacío")
    void testValidatePhone_NullOrEmpty() {
        // El método permite null o vacío
        assertDoesNotThrow(() -> {
            validationService.validatePhone(null);
            validationService.validatePhone("");
        });
    }

    // ==================== PRUEBAS DE ID ====================
    
    @Test
    @DisplayName("Debe validar ID correcto")
    void testValidateId_Valid() {
        assertDoesNotThrow(() -> 
            validationService.validateId(1L)
        );
    }

    @Test
    @DisplayName("Debe fallar con ID nulo")
    void testValidateId_Null() {
        assertThrows(
            NullPointerException.class,
            () -> validationService.validateId(null)
        );
    }

    @Test
    @DisplayName("Debe fallar con ID cero o negativo")
    void testValidateId_ZeroOrNegative() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateId(0L)
        );
        
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateId(-1L)
        );
    }

    // ==================== PRUEBAS DE RANGO ====================
    
    @Test
    @DisplayName("Debe validar rango correcto")
    void testValidateRange_Valid() {
        assertDoesNotThrow(() -> 
            validationService.validateRange(50, 0, 100, "Edad")
        );
    }

    @Test
    @DisplayName("Debe fallar fuera de rango mínimo")
    void testValidateRange_BelowMin() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateRange(-5, 0, 100, "Edad")
        );
        
        assertTrue(exception.getMessage().contains("mayor o igual"));
    }

    @Test
    @DisplayName("Debe fallar fuera de rango máximo")
    void testValidateRange_AboveMax() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateRange(150, 0, 100, "Edad")
        );
        
        assertTrue(exception.getMessage().contains("menor o igual"));
    }

    // ==================== PRUEBAS DE CANTIDAD POSITIVA ====================
    
    @Test
    @DisplayName("Debe validar cantidad positiva")
    void testValidatePositiveQuantity_Valid() {
        assertDoesNotThrow(() -> 
            validationService.validatePositiveQuantity(5, "Cantidad")
        );
    }

    @Test
    @DisplayName("Debe fallar con cantidad cero o negativa")
    void testValidatePositiveQuantity_Invalid() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePositiveQuantity(0, "Cantidad")
        );
        
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validatePositiveQuantity(-1, "Cantidad")
        );
    }

    // ==================== PRUEBAS DE SANITIZACIÓN ====================
    
    @Test
    @DisplayName("Debe sanitizar string con espacios múltiples")
    void testSanitizeString() {
        String result = validationService.sanitizeString("  Hola    Mundo  ");
        assertEquals("Hola Mundo", result);
    }

    @Test
    @DisplayName("Debe retornar null para string vacío o nulo")
    void testSanitizeString_NullOrEmpty() {
        assertNull(validationService.sanitizeString(null));
        assertNull(validationService.sanitizeString(""));
        // String con solo espacios retorna cadena vacía después de trim
        assertEquals("", validationService.sanitizeString("   "));
    }

    @Test
    @DisplayName("Debe validar y trimear string correctamente")
    void testValidateAndTrimString() {
        String result = validationService.validateAndTrimString("  Hola  ", "Nombre");
        assertEquals("Hola", result);
    }

    @Test
    @DisplayName("Debe fallar con string solo de espacios")
    void testValidateAndTrimString_OnlySpaces() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validationService.validateAndTrimString("    ", "Nombre")
        );
    }
}
