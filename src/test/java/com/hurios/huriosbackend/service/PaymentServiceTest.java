package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurios.huriosbackend.document.PaymentTransactionDocument;
import com.hurios.huriosbackend.dto.PaymentDtos;
import com.hurios.huriosbackend.entity.*;
import com.hurios.huriosbackend.repository.*;
import com.hurios.huriosbackend.repository.mongo.PaymentTransactionRepository;
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
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - Pruebas Unitarias")
class PaymentServiceTest {

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

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Product testProduct;
    private PaymentDtos.ProcessPaymentRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Producto Test");
        testProduct.setPrice(100.0);
        testProduct.setStock(10);

        testRequest = new PaymentDtos.ProcessPaymentRequest();
        testRequest.setPaymentMethod("card");
        testRequest.setTotalPrice(100.0);
        testRequest.setShippingCost(10.0);
        testRequest.setFinalTotal(110.0);

        PaymentDtos.CheckoutInfo checkoutInfo = new PaymentDtos.CheckoutInfo();
        checkoutInfo.setFullName("Test User");
        checkoutInfo.setPhone("987654321");
        checkoutInfo.setDocumentType("dni");
        checkoutInfo.setDni("12345678");
        checkoutInfo.setDeliveryMethod("delivery");
        checkoutInfo.setDeliveryAddress("Calle Test 123");
        checkoutInfo.setDeliveryDistrict("Lima");
        testRequest.setCheckoutInfo(checkoutInfo);

        List<PaymentDtos.OrderItem> items = new ArrayList<>();
        PaymentDtos.OrderItem item = new PaymentDtos.OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setPrice(100.0);
        items.add(item);
        testRequest.setItems(items);
    }

    @Test
    @DisplayName("Debe procesar el pago exitosamente cuando hay stock disponible")
    void testProcessPayment_Success() throws JsonProcessingException {
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));

        when(productRepository.findById(1L))
            .thenReturn(Optional.of(testProduct));

        Sale savedSale = new Sale();
        savedSale.setId(1L);
        savedSale.setUser(testUser);
        savedSale.setStatus("CONFIRMADO");
        savedSale.setTotal(110.0);

        when(saleRepository.save(any(Sale.class)))
            .thenReturn(savedSale);

        when(objectMapper.writeValueAsString(any()))
            .thenReturn("{}");

        PaymentDtos.ProcessPaymentResponse response =
            paymentService.processPayment(testRequest, "test@example.com");

        assertNotNull(response, "La respuesta no debe ser null");
        assertTrue(response.isSuccess(), "El pago debe ser exitoso");
        assertEquals("Pago procesado exitosamente", response.getMessage());
        assertEquals(1L, response.getOrderId());

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(productRepository, times(2)).findById(1L);
        verify(saleRepository, times(2)).save(any(Sale.class));
        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransactionDocument.class));

        assertEquals(8, testProduct.getStock(), "El stock debe decrementarse de 10 a 8");
    }

    @Test
    @DisplayName("createCheckoutPreference debe crear orden PENDIENTE sin descontar stock y guardar transaccion en MongoDB")
    void testCreateCheckoutPreference_CreatesPendingOrderWithoutDeductingStock() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));

        Sale pendingSale = new Sale();
        pendingSale.setId(50L);
        pendingSale.setUser(testUser);
        pendingSale.setStatus("PENDIENTE");
        pendingSale.setTotal(110.0);

        when(saleRepository.save(any(Sale.class))).thenReturn(pendingSale);
        when(paymentTransactionRepository.save(any(PaymentTransactionDocument.class))).thenAnswer(inv -> {
            PaymentTransactionDocument doc = inv.getArgument(0);
            doc.setId("mongo-tx-50");
            return doc;
        });

        PaymentDtos.CheckoutPreferenceResponse pref =
                paymentService.createCheckoutPreference(testRequest, "test@example.com");

        assertTrue(pref.isSuccess());
        assertEquals(50L, pref.getSaleId());
        assertEquals("mongo-tx-50", pref.getTransactionId());
        assertNotNull(pref.getInitPoint());
        assertEquals(10, testProduct.getStock(), "El stock NO debe descontarse al crear la preferencia");
        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransactionDocument.class));
    }

    @Test
    @DisplayName("verifyPayment debe confirmar la orden y descontar stock cuando el pago es aprobado")
    void testVerifyPayment_ConfirmsOrderAndDeductsStock() {
        Sale pendingSale = new Sale();
        pendingSale.setId(50L);
        pendingSale.setUser(testUser);
        pendingSale.setStatus("PENDIENTE");
        pendingSale.setTotal(110.0);

        SaleItem saleItem = new SaleItem();
        saleItem.setProduct(testProduct);
        saleItem.setQuantity(2);
        saleItem.setUnitPrice(100.0);
        pendingSale.addItem(saleItem);

        PaymentTransactionDocument existingTx = new PaymentTransactionDocument(50L, 1L, "test@example.com", 110.0, "mercadopago");
        existingTx.setId("mongo-tx-50");

        when(saleRepository.findById(50L)).thenReturn(Optional.of(pendingSale));
        when(paymentTransactionRepository.findBySaleId(50L)).thenReturn(Optional.of(existingTx));
        when(paymentTransactionRepository.save(any(PaymentTransactionDocument.class))).thenReturn(existingTx);

        PaymentDtos.PaymentVerificationResponse verified =
                paymentService.verifyPayment("MP-999", "approved", "50", "SANDBOX-PREF-50");

        assertTrue(verified.isApproved());
        assertEquals("CONFIRMADO", verified.getStatus());
        assertEquals(8, testProduct.getStock(), "El stock debe descontarse de 10 a 8 al aprobarse el pago");
        verify(saleRepository, times(1)).save(pendingSale);
        verify(paymentTransactionRepository, times(1)).save(existingTx);
    }

    @Test
    @DisplayName("Debe fallar cuando no hay suficiente stock")
    void testProcessPayment_InsufficientStock() {
        testProduct.setStock(1);

        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));

        when(productRepository.findById(1L))
            .thenReturn(Optional.of(testProduct));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> paymentService.processPayment(testRequest, "test@example.com"),
            "Debe lanzar RuntimeException cuando no hay stock"
        );

        assertTrue(exception.getMessage().contains("Stock insuficiente"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("Debe fallar cuando el usuario no existe")
    void testProcessPayment_UserNotFound() {
        when(userRepository.findByEmail("noexiste@example.com"))
            .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> paymentService.processPayment(testRequest, "noexiste@example.com")
        );

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("Debe fallar cuando el producto no existe")
    void testProcessPayment_ProductNotFound() {
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));

        when(productRepository.findById(1L))
            .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> paymentService.processPayment(testRequest, "test@example.com")
        );

        assertTrue(exception.getMessage().contains("Producto no encontrado"));
    }

    @Test
    @DisplayName("Debe obtener todas las ventas de un usuario")
    void testGetUserSales_Success() {
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

        List<Sale> result = paymentService.getUserSales("test@example.com");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(saleRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Debe obtener todas las ventas del sistema")
    void testGetAllSales_Success() {
        List<Sale> allSales = new ArrayList<>();
        allSales.add(new Sale());
        allSales.add(new Sale());
        allSales.add(new Sale());

        when(saleRepository.findAll())
            .thenReturn(allSales);

        List<Sale> result = paymentService.getAllSales();

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(saleRepository, times(1)).findAll();
    }
}
