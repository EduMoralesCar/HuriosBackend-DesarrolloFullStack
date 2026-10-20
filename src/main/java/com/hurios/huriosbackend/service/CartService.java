package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.document.CartDocument;
import com.hurios.huriosbackend.document.CartDocument.CartItem;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.repository.mongo.CartRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar el carrito de compras persistente en MongoDB ("carts").
 */
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
    }

    /**
     * Obtiene el carrito del usuario por email o devuelve uno vacío si aún no existe.
     */
    public CartDocument getCartByEmail(String userEmail) {
        return cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    Long userId = resolveUserId(userEmail);
                    return new CartDocument(userEmail, userId);
                });
    }

    /**
     * Reemplaza la lista de productos del carrito del usuario en MongoDB.
     */
    public CartDocument updateCart(String userEmail, List<CartItem> items) {
        CartDocument cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> new CartDocument(userEmail, resolveUserId(userEmail)));

        List<CartItem> validItems = sanitizeItems(items);
        cart.setItems(validItems);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * Fusiona el carrito local del navegador con el carrito almacenado en MongoDB al iniciar sesión.
     */
    public CartDocument mergeCart(String userEmail, List<CartItem> localItems) {
        CartDocument cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> new CartDocument(userEmail, resolveUserId(userEmail)));

        Map<Long, CartItem> mergedMap = new LinkedHashMap<>();

        if (cart.getItems() != null) {
            for (CartItem existing : cart.getItems()) {
                if (existing != null && existing.getId() != null && existing.getQuantity() != null && existing.getQuantity() > 0) {
                    mergedMap.put(existing.getId(), existing);
                }
            }
        }

        for (CartItem local : sanitizeItems(localItems)) {
            CartItem existing = mergedMap.get(local.getId());
            if (existing == null) {
                mergedMap.put(local.getId(), local);
            } else {
                int mergedQty = Math.max(existing.getQuantity(), local.getQuantity());
                if (local.getStock() != null && local.getStock() > 0) {
                    mergedQty = Math.min(mergedQty, local.getStock());
                }
                existing.setQuantity(mergedQty);
                if (local.getPrice() != null) {
                    existing.setPrice(local.getPrice());
                }
                if (local.getName() != null) {
                    existing.setName(local.getName());
                }
                if (local.getImageUrl() != null) {
                    existing.setImageUrl(local.getImageUrl());
                }
            }
        }

        cart.setItems(new ArrayList<>(mergedMap.values()));
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * Vacía el carrito del usuario en MongoDB.
     */
    public CartDocument clearCart(String userEmail) {
        CartDocument cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> new CartDocument(userEmail, resolveUserId(userEmail)));
        cart.setItems(new ArrayList<>());
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    private List<CartItem> sanitizeItems(List<CartItem> items) {
        List<CartItem> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (CartItem item : items) {
            if (item != null && item.getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private Long resolveUserId(String userEmail) {
        if (userEmail == null) {
            return null;
        }
        return userRepository.findByEmail(userEmail)
                .map(User::getId)
                .orElse(null);
    }
}
