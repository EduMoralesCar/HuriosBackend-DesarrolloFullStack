package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.repository.ProductRepository;
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
 * Pruebas unitarias para ProductService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Pruebas Unitarias")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Producto Test");
        testProduct.setPrice(100.0);
        testProduct.setDescription("Descripción test");
        testProduct.setStock(10);
        testProduct.setImageUrl("http://example.com/image.jpg");
    }

    // ==================== PRUEBAS DE LISTADO ====================

    @Test
    @DisplayName("Debe obtener todos los productos")
    void testFindAll_Success() {
        // ARRANGE
        List<Product> products = new ArrayList<>();
        products.add(testProduct);
        products.add(new Product());
        
        when(productRepository.findAll()).thenReturn(products);

        // ACT
        List<Product> result = productService.findAll();

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debe obtener producto por ID válido")
    void testFindById_Success() {
        // ARRANGE
        doNothing().when(validationService).validateId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // ACT
        Optional<Product> result = productService.findById(1L);

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("Producto Test", result.get().getName());
        verify(validationService, times(1)).validateId(1L);
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío para ID inexistente")
    void testFindById_NotFound() {
        // ARRANGE
        doNothing().when(validationService).validateId(999L);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT
        Optional<Product> result = productService.findById(999L);

        // ASSERT
        assertFalse(result.isPresent());
    }

    // ==================== PRUEBAS DE BÚSQUEDA ====================

    @Test
    @DisplayName("Debe buscar productos por nombre")
    void testSearchByName_Found() {
        // ARRANGE
        List<Product> allProducts = List.of(testProduct);
        when(productRepository.findAll()).thenReturn(allProducts);

        // ACT
        List<Product> result = productService.searchByName("Producto");

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Producto Test", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe buscar productos case insensitive")
    void testSearchByName_CaseInsensitive() {
        // ARRANGE
        List<Product> allProducts = List.of(testProduct);
        when(productRepository.findAll()).thenReturn(allProducts);

        // ACT
        List<Product> result = productService.searchByName("PRODUCTO");

        // ASSERT
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe retornar todos los productos si query está vacío")
    void testSearchByName_EmptyQuery() {
        // ARRANGE
        List<Product> allProducts = List.of(testProduct);
        when(productRepository.findAll()).thenReturn(allProducts);

        // ACT
        List<Product> result = productService.searchByName("");

        // ASSERT
        assertEquals(1, result.size());
    }

    // ==================== PRUEBAS DE CREACIÓN ====================

    @Test
    @DisplayName("Debe crear producto correctamente")
    void testCreateProduct_Success() {
        // ARRANGE
        doNothing().when(validationService).validateProductName(anyString());
        doNothing().when(validationService).validatePrice(anyDouble());
        doNothing().when(validationService).validateStock(anyInt());
        when(validationService.sanitizeString("Test")).thenReturn("Test");
        when(validationService.sanitizeString("Desc")).thenReturn("Desc");
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // ACT
        Product result = productService.createProduct(
            "Test", 100.0, "Desc", "http://test.com", 10
        );

        // ASSERT
        assertNotNull(result);
        verify(validationService).validateProductName("Test");
        verify(validationService).validatePrice(100.0);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Debe crear producto con stock 0 si no se proporciona")
    void testCreateProduct_DefaultStock() {
        // ARRANGE
        doNothing().when(validationService).validateProductName(anyString());
        doNothing().when(validationService).validatePrice(anyDouble());
        when(validationService.sanitizeString(anyString())).thenAnswer(i -> i.getArgument(0));
        
        Product savedProduct = new Product();
        savedProduct.setStock(0);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // ACT
        Product result = productService.createProduct(
            "Test", 100.0, "Desc", null, null
        );

        // ASSERT
        assertNotNull(result);
        assertEquals(0, result.getStock());
    }

    // ==================== PRUEBAS DE ACTUALIZACIÓN ====================

    @Test
    @DisplayName("Debe actualizar producto correctamente")
    void testUpdateProduct_Success() {
        // ARRANGE
        doNothing().when(validationService).validateId(1L);
        doNothing().when(validationService).validateProductName(anyString());
        doNothing().when(validationService).validatePrice(anyDouble());
        when(validationService.sanitizeString(anyString())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // ACT
        Product result = productService.updateProduct(
            1L, "Nuevo Nombre", 150.0, "Nueva Desc", "http://new.com"
        );

        // ASSERT
        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Debe fallar al actualizar producto inexistente")
    void testUpdateProduct_NotFound() {
        // ARRANGE
        doNothing().when(validationService).validateId(999L);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> productService.updateProduct(999L, "Test", null, null, null)
        );
        
        assertTrue(exception.getMessage().contains("no encontrado"));
    }

    // ==================== PRUEBAS DE STOCK ====================

    @Test
    @DisplayName("Debe agregar stock correctamente")
    void testAddStock_Success() {
        // ARRANGE
        doNothing().when(validationService).validateId(1L);
        doNothing().when(validationService).validatePositiveQuantity(5, "Cantidad a agregar");
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // ACT
        Product result = productService.addStock(1L, 5);

        // ASSERT
        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Debe reducir stock correctamente")
    void testReduceStock_Success() {
        // ARRANGE
        doNothing().when(validationService).validateId(1L);
        doNothing().when(validationService).validatePositiveQuantity(5, "Cantidad a reducir");
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // ACT
        Product result = productService.reduceStock(1L, 5);

        // ASSERT
        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Debe fallar al reducir stock insuficiente")
    void testReduceStock_InsufficientStock() {
        // ARRANGE
        testProduct.setStock(5);
        doNothing().when(validationService).validateId(1L);
        doNothing().when(validationService).validatePositiveQuantity(10, "Cantidad a reducir");
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> productService.reduceStock(1L, 10)
        );
        
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Debe verificar stock disponible correctamente")
    void testHasStock_Available() {
        // ARRANGE
        doNothing().when(validationService).validateId(1L);
        doNothing().when(validationService).validatePositiveQuantity(5, "Cantidad");
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // ACT
        boolean result = productService.hasStock(1L, 5);

        // ASSERT
        assertTrue(result);
    }

    @Test
    @DisplayName("Debe verificar stock no disponible")
    void testHasStock_NotAvailable() {
        // ARRANGE
        testProduct.setStock(3);
        doNothing().when(validationService).validateId(1L);
        doNothing().when(validationService).validatePositiveQuantity(5, "Cantidad");
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // ACT
        boolean result = productService.hasStock(1L, 5);

        // ASSERT
        assertFalse(result);
    }

    // ==================== PRUEBAS DE ELIMINACIÓN ====================

    @Test
    @DisplayName("Debe eliminar producto correctamente")
    void testDeleteProduct_Success() {
        // ARRANGE
        doNothing().when(validationService).validateId(1L);
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        // ACT
        assertDoesNotThrow(() -> productService.deleteProduct(1L));

        // ASSERT
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Debe fallar al eliminar producto inexistente")
    void testDeleteProduct_NotFound() {
        // ARRANGE
        doNothing().when(validationService).validateId(999L);
        when(productRepository.existsById(999L)).thenReturn(false);

        // ACT & ASSERT
        assertThrows(
            RuntimeException.class,
            () -> productService.deleteProduct(999L)
        );
    }

    // ==================== PRUEBAS DE UTILIDADES ====================

    @Test
    @DisplayName("Debe obtener productos con bajo stock")
    void testGetLowStockProducts() {
        // ARRANGE
        Product lowStock = new Product();
        lowStock.setStock(3);
        List<Product> allProducts = List.of(testProduct, lowStock);
        when(productRepository.findAll()).thenReturn(allProducts);

        // ACT
        List<Product> result = productService.getLowStockProducts(5);

        // ASSERT
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe obtener productos sin stock")
    void testGetOutOfStockProducts() {
        // ARRANGE
        Product noStock = new Product();
        noStock.setStock(0);
        List<Product> allProducts = List.of(testProduct, noStock);
        when(productRepository.findAll()).thenReturn(allProducts);

        // ACT
        List<Product> result = productService.getOutOfStockProducts();

        // ASSERT
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe contar productos totales")
    void testCountProducts() {
        // ARRANGE
        when(productRepository.count()).thenReturn(10L);

        // ACT
        long result = productService.countProducts();

        // ASSERT
        assertEquals(10L, result);
    }

    @Test
    @DisplayName("Debe verificar que producto existe")
    void testProductExists_True() {
        // ARRANGE
        when(productRepository.existsById(1L)).thenReturn(true);

        // ACT
        boolean result = productService.productExists(1L);

        // ASSERT
        assertTrue(result);
    }

    @Test
    @DisplayName("Debe verificar que producto no existe")
    void testProductExists_False() {
        // ARRANGE
        when(productRepository.existsById(999L)).thenReturn(false);

        // ACT
        boolean result = productService.productExists(999L);

        // ASSERT
        assertFalse(result);
    }

    @Test
    @DisplayName("Debe retornar false para ID nulo")
    void testProductExists_NullId() {
        // ACT
        boolean result = productService.productExists(null);

        // ASSERT
        assertFalse(result);
        verify(productRepository, never()).existsById(any());
    }
}
