package com.hurios.huriosbackend.repository.mongo;

import com.hurios.huriosbackend.document.ProductReviewDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio MongoDB para la colección "product_reviews".
 */
@Repository
public interface ProductReviewRepository extends MongoRepository<ProductReviewDocument, String> {

    List<ProductReviewDocument> findByProductIdOrderByCreatedAtDesc(Long productId);

    Optional<ProductReviewDocument> findByProductIdAndUserEmail(Long productId, String userEmail);
}
