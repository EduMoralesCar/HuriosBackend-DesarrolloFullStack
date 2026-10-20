package com.hurios.huriosbackend.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Documento MongoDB que almacena las reseñas y calificaciones de productos
 * en la colección "product_reviews".
 */
@Document(collection = "product_reviews")
public class ProductReviewDocument {

    @Id
    private String id;

    @Indexed
    private Long productId;

    @Indexed
    private Long userId;

    @Indexed
    private String userEmail;

    private String userName;

    private Integer rating; // 1 a 5 estrellas

    private String comment;

    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProductReviewDocument() {
    }

    public ProductReviewDocument(Long productId, Long userId, String userEmail,
                                 String userName, Integer rating, String comment) {
        this.productId = productId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
