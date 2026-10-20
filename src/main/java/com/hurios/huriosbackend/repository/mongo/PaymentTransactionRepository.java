package com.hurios.huriosbackend.repository.mongo;

import com.hurios.huriosbackend.document.PaymentTransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio MongoDB para la colección "payment_transactions".
 */
@Repository
public interface PaymentTransactionRepository extends MongoRepository<PaymentTransactionDocument, String> {

    Optional<PaymentTransactionDocument> findBySaleId(Long saleId);

    Optional<PaymentTransactionDocument> findByPreferenceId(String preferenceId);

    Optional<PaymentTransactionDocument> findByMpPaymentId(String mpPaymentId);

    List<PaymentTransactionDocument> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<PaymentTransactionDocument> findAllByOrderByCreatedAtDesc();
}
