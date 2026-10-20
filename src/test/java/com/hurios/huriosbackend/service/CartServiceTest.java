package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.document.CartDocument;
import com.hurios.huriosbackend.document.CartDocument.CartItem;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.repository.mongo.CartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CartService (MongoDB).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService (MongoDB) - Pruebas Unitarias")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("getCartByEmail debe retornar un carrito vacio cuando no existe en MongoDB")
    void testGetCartByEmail_ReturnsEmptyWhenNotFound() {
        User user = new User();
        user.setId(10L);
        user.setEmail("cliente@hurios.com");

        when(cartRepository.findByUserEmail("cliente@hurios.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("cliente@hurios.com")).thenReturn(Optional.of(user));

        CartDocument cart = cartService.getCartByEmail("cliente@hurios.com");

        assertEquals("cliente@hurios.com", cart.getUserEmail());
        assertEquals(10L, cart.getUserId());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    @DisplayName("updateCart debe guardar la lista de items validos en MongoDB")
    void testUpdateCart_SavesItems() {
        when(cartRepository.findByUserEmail("cliente@hurios.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("cliente@hurios.com")).thenReturn(Optional.empty());
        when(cartRepository.save(any(CartDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem item1 = new CartItem(1L, "Pastillas de Freno", 120.0, 2, "/img/1.webp");
        CartDocument result = cartService.updateCart("cliente@hurios.com", List.of(item1));

        assertEquals(1, result.getItems().size());
        assertEquals("Pastillas de Freno", result.getItems().get(0).getName());
        assertEquals(2, result.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(CartDocument.class));
    }

    @Test
    @DisplayName("mergeCart debe combinar items existentes en MongoDB con items locales del navegador")
    void testMergeCart_CombinesItems() {
        CartDocument existing = new CartDocument("cliente@hurios.com", 10L);
        existing.setItems(List.of(new CartItem(1L, "Filtro de Aire", 45.0, 1, "/img/1.webp")));

        when(cartRepository.findByUserEmail("cliente@hurios.com")).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(CartDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        List<CartItem> localItems = List.of(
                new CartItem(1L, "Filtro de Aire", 45.0, 3, "/img/1.webp"),
                new CartItem(2L, "Bujia Iridium", 35.0, 2, "/img/2.webp")
        );

        CartDocument merged = cartService.mergeCart("cliente@hurios.com", localItems);

        assertEquals(2, merged.getItems().size());
        assertEquals(3, merged.getItems().get(0).getQuantity());
        assertEquals(2L, merged.getItems().get(1).getId());
    }

    @Test
    @DisplayName("clearCart debe vaciar la lista de items del usuario")
    void testClearCart_EmptiesItems() {
        CartDocument existing = new CartDocument("cliente@hurios.com", 10L);
        existing.setItems(List.of(new CartItem(1L, "Filtro de Aire", 45.0, 1, "/img/1.webp")));

        when(cartRepository.findByUserEmail("cliente@hurios.com")).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(CartDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        CartDocument cleared = cartService.clearCart("cliente@hurios.com");

        assertTrue(cleared.getItems().isEmpty());
        verify(cartRepository, times(1)).save(any(CartDocument.class));
    }
}
