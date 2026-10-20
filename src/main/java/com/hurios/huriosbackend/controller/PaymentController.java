package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.dto.PaymentDtos;
import com.hurios.huriosbackend.entity.Sale;
import com.hurios.huriosbackend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

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
}
