package com.hurios.huriosbackend.repository.mongo;

import com.hurios.huriosbackend.document.CartDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio MongoDB para la colección "carts".
 */
@Repository
public interface CartRepository extends MongoRepository<CartDocument, String> {

    Optional<CartDocument> findByUserEmail(String userEmail);

    Optional<CartDocument> findByUserId(Long userId);

    void deleteByUserEmail(String userEmail);
}
