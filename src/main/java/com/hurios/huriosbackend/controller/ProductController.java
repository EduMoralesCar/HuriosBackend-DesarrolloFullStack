package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.service.ProductService;
import com.hurios.huriosbackend.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/products")
/**
 * ProductController - controlador sencillo y seguro para productos.
 * - Devolvemos ResponseEntity<Object> para poder enviar Product o Map de error.
 */
public class ProductController {
    private final ProductRepository productRepository;
    private final ValidationService validationService;
    private final ProductService productService;
    private final com.hurios.huriosbackend.service.CatalogSyncService catalogSyncService;

    public ProductController(ProductRepository productRepository, 
                             ValidationService validationService,
                             ProductService productService,
                             com.hurios.huriosbackend.service.CatalogSyncService catalogSyncService) {
        this.productRepository = productRepository;
        this.validationService = validationService;
        this.productService = productService;
        this.catalogSyncService = catalogSyncService;
    }

    // POST /products/sync-api -> Sincroniza y puebla automáticamente productos de repuestos vía API
    @PostMapping("/sync-api")
    public ResponseEntity<?> syncCatalogFromApi() {
        try {
            var synced = catalogSyncService.syncCatalogFromAI();
            return ResponseEntity.ok(Map.of(
                    "message", "Catálogo sincronizado exitosamente con la API",
                    "nuevos_productos", synced.size(),
                    "total_productos", productRepository.count()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /products -> Lista de todos los productos
    @GetMapping
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    // GET /products/{id} -> Muestra los datos del producto seleccionado por su ID
    @GetMapping("/{id}")
    public ResponseEntity<Object> getProducto(@PathVariable Long id) {
        Optional<Product> maybe = productRepository.findById(id);
        if (maybe.isPresent()) {
            return ResponseEntity.ok(maybe.get());
        }

        return ResponseEntity.status(404).body(
                Map.of("error", "Producto no encontrado")
        );
    }
    // GET /products/search?q=query -> Sirve para buscar productos por nombre
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(productRepository.findAll());
        }

        return ResponseEntity.ok(
                productRepository.findAll().stream()
                        .filter(p -> p.getName().toLowerCase().contains(q.toLowerCase()))
                        .toList()
        );
    }

    // PUT /products/{id}/add-stock -> Agrega stock a un producto existente
    @PutMapping("/{id}/add-stock")
    public ResponseEntity<?> addStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Optional<Product> maybe = productRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Producto no encontrado")
            );
        }

        Integer quantity = body.get("quantity");
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "La cantidad debe ser mayor a 0")
            );
        }

        Product product = maybe.get();
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        product.setStock(currentStock + quantity);
        productRepository.save(product);

        return ResponseEntity.ok(Map.of(
                "message", "Stock actualizado correctamente",
                "newStock", product.getStock()
        ));
    }

    // POST /products -> Permite agregar un nuevo producto o repuesto en el inventario y a la venta
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");

            try {
                validationService.validateProductName(name);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", e.getMessage())
                );
            }

            Object priceObj = body.get("price");
            Product product = new Product();
            product.setName(name.trim());

            Double price;
            if (priceObj instanceof Number) {
                price = ((Number) priceObj).doubleValue();
            } else {
                try {
                    price = Double.parseDouble(priceObj.toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "El precio debe ser un numero valido")
                    );
                }
            }
            product.setPrice(price);

            String description = (String) body.get("description");
            if (description != null && !description.trim().isEmpty()) {
                product.setDescription(description.trim());
            }

            String imageUrl = (String) body.get("imageUrl");
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                product.setImageUrl(imageUrl.trim());
            }

            String category = (String) body.get("category");
            if (category != null && !category.trim().isEmpty()) {
                product.setCategory(category.trim());
            }

            Object stockObj = body.get("stock");
            if (stockObj != null) {
                Integer stock;
                if (stockObj instanceof Number) {
                    stock = ((Number) stockObj).intValue();
                } else {
                    try {
                        stock = Integer.parseInt(stockObj.toString());
                    } catch (NumberFormatException e) {
                        stock = 0;
                    }
                }
                product.setStock(stock);
            } else {
                product.setStock(0);
            }

            Product savedProduct = productRepository.save(product);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error al crear el producto: " + e.getMessage())
            );
        }
    }

    // PUT /products/{id} -> Sirve para actualizar el nombre, descripcion y/o imagen de un producto existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<Product> maybe = productRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Producto no encontrado")
            );
        }

        try {
            Product product = maybe.get();

            String name = (String) body.get("name");
            if (name != null && !name.trim().isEmpty()) {
                try {
                    validationService.validateProductName(name);
                    product.setName(name.trim());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", e.getMessage())
                    );
                }
            }

            String description = (String) body.get("description");
            if (description != null) {
                product.setDescription(description.trim().isEmpty() ? null : description.trim());
            }

            Object priceObj = body.get("price");
            if (priceObj != null) {
                Double price;
                if (priceObj instanceof Number) {
                    price = ((Number) priceObj).doubleValue();
                } else {
                    try {
                        price = Double.parseDouble(priceObj.toString());
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body(
                                Map.of("error", "El precio debe ser un numero valido")
                        );
                    }
                }

                if (price <= 0) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "El precio debe ser mayor a 0")
                    );
                }
                product.setPrice(price);
            }

            String imageUrl = (String) body.get("imageUrl");
            if (imageUrl != null) {
                product.setImageUrl(imageUrl.trim().isEmpty() ? null : imageUrl.trim());
            }

            String category = (String) body.get("category");
            if (category != null) {
                product.setCategory(category.trim().isEmpty() ? null : category.trim());
            }

            Product updatedProduct = productRepository.save(product);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error al actualizar el producto: " + e.getMessage())
            );
        }
    }

    // DELETE /products/{id} -> Sirve para eliminar un producto estableciendo su ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        Optional<Product> maybe = productRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Producto no encontrado")
            );
        }

        try {
            productRepository.deleteById(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Producto eliminado correctamente",
                    "id", id
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error al eliminar el producto: " + e.getMessage())
            );
        }
    }
}
