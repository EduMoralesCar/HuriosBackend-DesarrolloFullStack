package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurios.huriosbackend.dto.PaymentDtos;
import com.hurios.huriosbackend.entity.*;
import com.hurios.huriosbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PaymentService usando JUnit 5 y Mockito
 * 
 * Patrón TDD (Test-Driven Development):
 * 1. Escribir la prueba primero (esto define el comportamiento esperado)
 * 2. Ejecutar la prueba (debe fallar si no existe la implementación)
 * 3. Escribir el código mínimo para que la prueba pase
 * 4. Refactorizar el código manteniendo las pruebas pasando
 */
@ExtendWith(MockitoExtension.class) // Habilita Mockito con JUnit 5
@DisplayName("PaymentService - Pruebas Unitarias")
class PaymentServiceTest {

    // @Mock crea un "mock" (objeto simulado) de las dependencias
    @Mock
    private SaleRepository saleRepository;
    
    @Mock
    private SaleItemRepository saleItemRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ObjectMapper objectMapper;

    // @InjectMocks crea una instancia de PaymentService e inyecta los mocks
    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Product testProduct;
    private PaymentDtos.ProcessPaymentRequest testRequest;

    /**
     * @BeforeEach: Se ejecuta antes de cada prueba
     * Aquí configuramos los datos de prueba que usaremos en múltiples tests
     */
    @BeforeEach
    void setUp() {
        // Crear usuario de prueba
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        // Crear producto de prueba con stock disponible
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Producto Test");
        testProduct.setPrice(100.0);
        testProduct.setStock(10);

        // Crear request de pago de prueba
        testRequest = new PaymentDtos.ProcessPaymentRequest();
        testRequest.setPaymentMethod("card");
        testRequest.setTotalPrice(100.0);
        testRequest.setShippingCost(10.0);
        testRequest.setFinalTotal(110.0);

        // Configurar checkout info
        PaymentDtos.CheckoutInfo checkoutInfo = new PaymentDtos.CheckoutInfo();
        checkoutInfo.setFullName("Test User");
        checkoutInfo.setPhone("987654321");
        checkoutInfo.setDocumentType("dni");
        checkoutInfo.setDni("12345678");
        checkoutInfo.setDeliveryMethod("delivery");
        checkoutInfo.setDeliveryAddress("Calle Test 123");
        checkoutInfo.setDeliveryDistrict("Lima");
        testRequest.setCheckoutInfo(checkoutInfo);

        // Agregar item al carrito
        List<PaymentDtos.OrderItem> items = new ArrayList<>();
        PaymentDtos.OrderItem item = new PaymentDtos.OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setPrice(100.0);
        items.add(item);
        testRequest.setItems(items);
    }

    /**
     * PRUEBA 1: Procesar pago exitosamente
     * Escenario: Usuario con stock disponible realiza una compra
     * Resultado esperado: Se crea la venta y se descuenta el stock
     */
    @Test
    @DisplayName("Debe procesar el pago exitosamente cuando hay stock disponible")
    void testProcessPayment_Success() throws JsonProcessingException {
        // ARRANGE (Preparar): Configurar el comportamiento de los mocks
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        
        when(productRepository.findById(1L))
            .thenReturn(Optional.of(testProduct));
        
        // Configurar el mock para que devuelva una venta con ID
        Sale savedSale = new Sale();
        savedSale.setId(1L);
        savedSale.setUser(testUser);
        savedSale.setStatus("CONFIRMADO");
        savedSale.setTotal(110.0);
        
        when(saleRepository.save(any(Sale.class)))
            .thenReturn(savedSale);
        
        when(objectMapper.writeValueAsString(any()))
            .thenReturn("{}");

        // ACT (Actuar): Ejecutar el método que queremos probar
        PaymentDtos.ProcessPaymentResponse response = 
            paymentService.processPayment(testRequest, "test@example.com");

        // ASSERT (Verificar): Comprobar que el resultado es el esperado
        assertNotNull(response, "La respuesta no debe ser null");
        assertTrue(response.isSuccess(), "El pago debe ser exitoso");
        assertEquals("Pago procesado exitosamente", response.getMessage());
        assertEquals(1L, response.getOrderId());
        
        // Verificar que se llamaron los métodos esperados
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(productRepository, times(2)).findById(1L); // Se llama 2 veces
        verify(saleRepository, times(2)).save(any(Sale.class));
        
        // Verificar que se descontó el stock
        assertEquals(8, testProduct.getStock(), "El stock debe decrementarse de 10 a 8");
    }

    /**
     * PRUEBA 2: Falla cuando no hay suficiente stock
     * Escenario: Usuario intenta comprar más productos de los disponibles
     * Resultado esperado: Se lanza una excepción
     */
    @Test
    @DisplayName("Debe fallar cuando no hay suficiente stock")
    void testProcessPayment_InsufficientStock() {
        // ARRANGE: Configurar producto con poco stock
        testProduct.setStock(1); // Solo hay 1 unidad
        
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        
        when(productRepository.findById(1L))
            .thenReturn(Optional.of(testProduct));

        // ACT & ASSERT: Verificar que se lanza la excepción esperada
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> paymentService.processPayment(testRequest, "test@example.com"),
            "Debe lanzar RuntimeException cuando no hay stock"
        );
        
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
        
        // Verificar que NO se guardó ninguna venta
        verify(saleRepository, never()).save(any(Sale.class));
    }

    /**
     * PRUEBA 3: Falla cuando el usuario no existe
     * Escenario: Email de usuario no registrado
     * Resultado esperado: Se lanza una excepción
     */
    @Test
    @DisplayName("Debe fallar cuando el usuario no existe")
    void testProcessPayment_UserNotFound() {
        // ARRANGE: Simular que el usuario no existe
        when(userRepository.findByEmail("noexiste@example.com"))
            .thenReturn(Optional.empty());

        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> paymentService.processPayment(testRequest, "noexiste@example.com")
        );
        
        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    /**
     * PRUEBA 4: Falla cuando el producto no existe
     */
    @Test
    @DisplayName("Debe fallar cuando el producto no existe")
    void testProcessPayment_ProductNotFound() {
        // ARRANGE
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        
        when(productRepository.findById(1L))
            .thenReturn(Optional.empty());

        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> paymentService.processPayment(testRequest, "test@example.com")
        );
        
        assertTrue(exception.getMessage().contains("Producto no encontrado"));
    }

    /**
     * PRUEBA 5: Obtener ventas de un usuario
     */
    @Test
    @DisplayName("Debe obtener todas las ventas de un usuario")
    void testGetUserSales_Success() {
        // ARRANGE
        List<Sale> expectedSales = new ArrayList<>();
        Sale sale1 = new Sale();
        sale1.setId(1L);
        Sale sale2 = new Sale();
        sale2.setId(2L);
        expectedSales.add(sale1);
        expectedSales.add(sale2);
        
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        
        when(saleRepository.findByUserId(1L))
            .thenReturn(expectedSales);

        // ACT
        List<Sale> result = paymentService.getUserSales("test@example.com");

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(saleRepository, times(1)).findByUserId(1L);
    }

  
    /**
     * PRUEBA 8: Obtener todas las ventas (admin)
     */
    @Test
    @DisplayName("Debe obtener todas las ventas del sistema")
    void testGetAllSales_Success() {
        // ARRANGE
        List<Sale> allSales = new ArrayList<>();
        allSales.add(new Sale());
        allSales.add(new Sale());
        allSales.add(new Sale());
        
        when(saleRepository.findAll())
            .thenReturn(allSales);

        // ACT
        List<Sale> result = paymentService.getAllSales();

        // ASSERT
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(saleRepository, times(1)).findAll();
    }
}
