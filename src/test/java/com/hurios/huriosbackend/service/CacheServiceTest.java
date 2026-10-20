package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CacheService.
 *
 * Se valida principalmente:
 * - Carga inicial desde repositorio.
 * - Reutilización de datos en caché.
 * - Invalidación de entradas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService - Pruebas Unitarias")
class CacheServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CacheService cacheService;

    @Test
    @DisplayName("Debe cargar producto desde repositorio solo una vez y luego usar caché")
    void testGetProduct_UsesCacheAfterFirstLoad() throws ExecutionException {
        // ARRANGE
        Product product = new Product();
        product.setId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // ACT
        Product firstCall = cacheService.getProduct(1L);
        Product secondCall = cacheService.getProduct(1L);

        // ASSERT
        assertNotNull(firstCall);
        assertSame(firstCall, secondCall, "El mismo objeto debe reutilizarse desde caché");
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe cargar usuario por email desde repositorio solo una vez y luego usar caché")
    void testGetUserByEmail_UsesCacheAfterFirstLoad() throws ExecutionException {
        // ARRANGE
        User user = new User();
        user.setId(5L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // ACT
        User firstCall = cacheService.getUserByEmail("test@example.com");
        User secondCall = cacheService.getUserByEmail("test@example.com");

        // ASSERT
        assertNotNull(firstCall);
        assertEquals("test@example.com", firstCall.getEmail());
        assertSame(firstCall, secondCall);
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Debe recargar producto después de invalidarlo")
    void testInvalidateProduct_ForcesReload() throws ExecutionException {
        // ARRANGE
        Product first = new Product();
        first.setId(10L);
        Product second = new Product();
        second.setId(10L);

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(first))
                .thenReturn(Optional.of(second));

        // ACT
        Product firstCall = cacheService.getProduct(10L);
        cacheService.invalidateProduct(10L);
        Product secondCall = cacheService.getProduct(10L);

        // ASSERT
        assertNotSame(firstCall, secondCall, "Después de invalidar, debe recargarse desde repositorio");
        verify(productRepository, times(2)).findById(10L);
    }

    @Test
    @DisplayName("Debe limpiar completamente el caché de productos y usuarios")
    void testClearCaches_DoesNotThrow() throws ExecutionException {
        // ARRANGE: llenar mínimamente la caché
        Product product = new Product();
        product.setId(99L);
        when(productRepository.findById(99L)).thenReturn(Optional.of(product));

        cacheService.getProduct(99L);

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            cacheService.clearProductCache();
            cacheService.clearUserCache();
        });
    }

    @Test
    @DisplayName("Debe retornar estadísticas de caché no vacías")
    void testCacheStats_NotEmpty() throws ExecutionException {
        // Forzar al menos una operación
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        try {
            cacheService.getProduct(1L);
        } catch (ExecutionException e) {
            // Es posible que lance por Optional.empty(), ignoramos para esta prueba
        }

        String productStats = cacheService.getProductCacheStats();
        String userStats = cacheService.getUserCacheStats();

        assertNotNull(productStats);
        assertTrue(productStats.contains("Product Cache Stats"));
        assertNotNull(userStats);
        assertTrue(userStats.contains("User Cache Stats"));
    }
}
