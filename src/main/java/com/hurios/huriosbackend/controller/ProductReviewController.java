package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.document.ProductReviewDocument;
import com.hurios.huriosbackend.service.ProductReviewService;
import com.hurios.huriosbackend.websocket.RealtimeNotificationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para gestionar las reseñas de productos en MongoDB ("product_reviews").
 */
@RestController
@RequestMapping("/products/{productId}/reviews")
public class ProductReviewController {

    private final ProductReviewService reviewService;

    @Autowired(required = false)
    private RealtimeNotificationHandler realtimeNotificationHandler;

    public ProductReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public static class CreateReviewRequest {
        private Integer rating;
        private String comment;

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviewsSummary(productId));
    }

    @PostMapping
    public ResponseEntity<?> createReview(
            @PathVariable Long productId,
            @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesión para publicar una reseña"));
        }

        try {
            String userEmail = authentication.getName();
            ProductReviewDocument saved = reviewService.createOrUpdateReview(
                    productId,
                    userEmail,
                    request.getRating(),
                    request.getComment()
            );
            Map<String, Object> summary = reviewService.getProductReviewsSummary(productId);
            summary.put("savedReview", saved);

            if (realtimeNotificationHandler != null) {
                realtimeNotificationHandler.broadcastEvent(
                        "NEW_REVIEW",
                        saved.getUserName() + " calificó un repuesto con " + saved.getRating() + " estrellas",
                        Map.of(
                                "productId", productId,
                                "rating", saved.getRating(),
                                "userName", saved.getUserName()
                        )
                );
            }

            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la reseña: " + e.getMessage()));
        }
    }
}
