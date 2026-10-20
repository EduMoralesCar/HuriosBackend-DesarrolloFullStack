package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.document.ProductReviewDocument;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.repository.mongo.ProductReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProductReviewService (MongoDB).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewService (MongoDB) - Pruebas Unitarias")
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductReviewService reviewService;

    @Test
    @DisplayName("getProductReviewsSummary debe calcular el promedio de estrellas y total de resenas")
    void testGetProductReviewsSummary_CalculatesAverage() {
        ProductReviewDocument r1 = new ProductReviewDocument(1L, 10L, "a@test.com", "Juan", 5, "Excelente");
        ProductReviewDocument r2 = new ProductReviewDocument(1L, 11L, "b@test.com", "Ana", 4, "Muy bueno");

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r1, r2));

        Map<String, Object> summary = reviewService.getProductReviewsSummary(1L);

        assertEquals(1L, summary.get("productId"));
        assertEquals(4.5, summary.get("averageRating"));
        assertEquals(2, summary.get("totalReviews"));
    }

    @Test
    @DisplayName("createOrUpdateReview debe guardar una nueva resena en MongoDB")
    void testCreateOrUpdateReview_SavesNewReview() {
        User user = new User();
        user.setId(10L);
        user.setEmail("cliente@hurios.com");
        user.setFullName("Carlos Perez");

        when(productRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findByEmail("cliente@hurios.com")).thenReturn(Optional.of(user));
        when(reviewRepository.findByProductIdAndUserEmail(1L, "cliente@hurios.com")).thenReturn(Optional.empty());
        when(reviewRepository.save(any(ProductReviewDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductReviewDocument saved = reviewService.createOrUpdateReview(
                1L, "cliente@hurios.com", 5, "Repuesto original de alta calidad"
        );

        assertEquals(1L, saved.getProductId());
        assertEquals(10L, saved.getUserId());
        assertEquals("Carlos Perez", saved.getUserName());
        assertEquals(5, saved.getRating());
        assertEquals("Repuesto original de alta calidad", saved.getComment());
        verify(reviewRepository, times(1)).save(any(ProductReviewDocument.class));
    }

    @Test
    @DisplayName("createOrUpdateReview debe lanzar excepcion si la calificacion esta fuera de 1 a 5")
    void testCreateOrUpdateReview_InvalidRating() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createOrUpdateReview(1L, "cliente@hurios.com", 6, "Comentario")
        );
        verify(reviewRepository, never()).save(any());
    }
}
