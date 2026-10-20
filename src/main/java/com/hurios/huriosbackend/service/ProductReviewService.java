package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.document.ProductReviewDocument;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.repository.mongo.ProductReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para gestionar reseñas y calificaciones de productos en MongoDB ("product_reviews").
 */
@Service
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductReviewService(
            ProductReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * Obtiene las reseñas de un producto junto con su promedio de calificación y total de reseñas.
     */
    public Map<String, Object> getProductReviewsSummary(Long productId) {
        List<ProductReviewDocument> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);

        double average = 0.0;
        if (!reviews.isEmpty()) {
            double sum = reviews.stream()
                    .mapToInt(r -> r.getRating() != null ? r.getRating() : 0)
                    .sum();
            average = Math.round((sum / reviews.size()) * 10.0) / 10.0;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("productId", productId);
        summary.put("averageRating", average);
        summary.put("totalReviews", reviews.size());
        summary.put("reviews", reviews);
        return summary;
    }

    /**
     * Crea o actualiza la reseña de un usuario para un producto en MongoDB.
     */
    public ProductReviewDocument createOrUpdateReview(
            Long productId,
            String userEmail,
            Integer rating,
            String comment
    ) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5 estrellas");
        }
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("El comentario no puede estar vacío");
        }
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Producto no encontrado: #" + productId);
        }

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        Long userId = userOpt.map(User::getId).orElse(null);
        String userName = userOpt
                .map(u -> (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getEmail())
                .orElse(userEmail);

        ProductReviewDocument review = reviewRepository.findByProductIdAndUserEmail(productId, userEmail)
                .orElseGet(() -> new ProductReviewDocument(productId, userId, userEmail, userName, rating, comment.trim()));

        review.setUserId(userId);
        review.setUserName(userName);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }
}
