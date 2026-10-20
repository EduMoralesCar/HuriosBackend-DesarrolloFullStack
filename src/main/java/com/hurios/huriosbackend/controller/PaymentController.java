package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.dto.PaymentDtos;
import com.hurios.huriosbackend.entity.Sale;
import com.hurios.huriosbackend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Inicia un flujo de pago con MercadoPago Checkout Pro y registra la transacción en MongoDB.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(
            @RequestBody PaymentDtos.ProcessPaymentRequest request,
            Authentication authentication
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            String userEmail = authentication.getName();
            PaymentDtos.CheckoutPreferenceResponse response =
                    paymentService.createCheckoutPreference(request, userEmail);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al iniciar checkout con MercadoPago: " + e.getMessage()));
        }
    }

    /**
     * Verifica el estado del pago al retornar desde MercadoPago y confirma la orden si fue aprobado.
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestParam(name = "payment_id", required = false) String paymentId,
            @RequestParam(name = "collection_id", required = false) String collectionId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "collection_status", required = false) String collectionStatus,
            @RequestParam(name = "external_reference", required = false) String externalReference,
            @RequestParam(name = "preference_id", required = false) String preferenceId
    ) {
        try {
            String effectivePaymentId = (paymentId != null && !paymentId.isBlank()) ? paymentId : collectionId;
            String effectiveStatus = (status != null && !status.isBlank()) ? status : collectionStatus;

            PaymentDtos.PaymentVerificationResponse verified = paymentService.verifyPayment(
                    effectivePaymentId,
                    effectiveStatus,
                    externalReference,
                    preferenceId
            );
            return ResponseEntity.ok(verified);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar el pago: " + e.getMessage()));
        }
    }

    /**
     * Webhook público para recibir notificaciones automáticas de MercadoPago en producción (Render).
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestParam Map<String, String> queryParams
    ) {
        Map<String, Object> result = paymentService.handleWebhook(payload, queryParams);
        return ResponseEntity.ok(result);
    }

    /**
     * Obtiene todas las transacciones registradas en la colección MongoDB "payment_transactions".
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }

    /**
     * Obtiene el documento de transacción en MongoDB asociado a una orden específica.
     */
    @GetMapping("/transactions/sale/{saleId}")
    public ResponseEntity<?> getTransactionBySaleId(@PathVariable Long saleId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        return paymentService.getTransactionBySaleId(saleId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Transacción no encontrada para la orden #" + saleId)));
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(
            @RequestBody PaymentDtos.ProcessPaymentRequest request,
            Authentication authentication
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String userEmail = authentication.getName();
            PaymentDtos.ProcessPaymentResponse response = paymentService.processPayment(request, userEmail);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al procesar el pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getUserOrders(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String userEmail = authentication.getName();
            List<Sale> sales = paymentService.getUserSales(userEmail);

            return ResponseEntity.ok(sales);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al obtener las ordenes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllSales(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            List<Sale> sales = paymentService.getAllSales();
            return ResponseEntity.ok(sales);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al obtener las ventas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSaleById(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            return ResponseEntity.ok(paymentService.getSaleById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
