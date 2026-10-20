package com.hurios.huriosbackend.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento MongoDB que almacena el carrito de compras persistente de cada usuario
 * en la colección "carts".
 */
@Document(collection = "carts")
public class CartDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userEmail;

    @Indexed
    private Long userId;

    private List<CartItem> items = new ArrayList<>();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public CartDocument() {
    }

    public CartDocument(String userEmail, Long userId) {
        this.userEmail = userEmail;
        this.userId = userId;
        this.items = new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Subdocumento que representa un producto dentro del carrito.
     */
    public static class CartItem {
        private Long id;
        private String name;
        private Double price;
        private String description;
        private String imageUrl;
        private Integer stock;
        private String category;
        private Integer quantity;

        public CartItem() {
        }

        public CartItem(Long id, String name, Double price, Integer quantity, String imageUrl) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.imageUrl = imageUrl;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getProductId() {
            return id;
        }

        public void setProductId(Long productId) {
            if (this.id == null) {
                this.id = productId;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
