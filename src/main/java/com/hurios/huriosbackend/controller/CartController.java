package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.document.CartDocument;
import com.hurios.huriosbackend.document.CartDocument.CartItem;
import com.hurios.huriosbackend.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para sincronizar el carrito de compras persistente en MongoDB ("carts").
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    public static class CartRequest {
        private List<CartItem> items;

        public List<CartItem> getItems() {
            return items != null ? items : Collections.emptyList();
        }

        public void setItems(List<CartItem> items) {
            this.items = items;
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        CartDocument cart = cartService.getCartByEmail(principal.getName());
        return ResponseEntity.ok(cart);
    }

    @PutMapping
    public ResponseEntity<?> updateCart(@RequestBody CartRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        CartDocument updated = cartService.updateCart(principal.getName(), request.getItems());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/merge")
    public ResponseEntity<?> mergeCart(@RequestBody CartRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        CartDocument merged = cartService.mergeCart(principal.getName(), request.getItems());
        return ResponseEntity.ok(merged);
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        CartDocument cleared = cartService.clearCart(principal.getName());
        return ResponseEntity.ok(cleared);
    }
}
