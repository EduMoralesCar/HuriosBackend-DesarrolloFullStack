package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ProductService - Servicio con lógica de negocio para productos
 * Responsabilidades:
 * - CRUD de productos
 * - Búsqueda y filtrado
 * - Gestión de stock
 * - Validaciones de negocio
 */
@Service
public class ProductService {
    private final ProductRepository repo;
    private final ValidationService validationService;
    public ProductService(ProductRepository repo, ValidationService validationService) {
        this.repo = repo;
        this.validationService = validationService;
    }
    /**
     * Obtener todos los productos
     */
    public List<Product> findAll() {
        return repo.findAll();
    }
    /**
     * Obtener producto por ID
     */
    public Optional<Product> findById(Long id) {
        validationService.validateId(id);
        return repo.findById(id);
    }
    /**
     * Buscar productos por nombre (case insensitive)
     */
    public List<Product> searchByName(String query){
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String lowerQuery = query.toLowerCase().trim();
        return repo.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * Crear nuevo producto
     */
    @Transactional
    public Product createProduct(String name, Double price, String description,
                                 String imageUrl, Integer stock) {
        // Validar campos requeridos
        validationService.validateProductName(name);
        validationService.validatePrice(price);
        // Validar stock si se proporciona
        if (stock != null) {
            validationService.validateStock(stock);
        }
        Product product = new Product();
        product.setName(validationService.sanitizeString(name));
        product.setPrice(price);
        // Campos opcionales
        if (description != null && !description.trim().isEmpty()) {
            product.setDescription(validationService.sanitizeString(description));
        }
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            product.setImageUrl(imageUrl.trim());
        }
        product.setStock(stock != null ? stock : 0);
        return repo.save(product);
    }
    /**
     * Actualizar producto existente
     */
    @Transactional
    public Product updateProduct(Long id, String name, Double price, String description, String imageUrl) {
        validationService.validateId(id);
        Product product = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        // Actualizar nombre si se proporciona
        if (name != null && !name.trim().isEmpty()) {
            validationService.validateProductName(name);
            product.setName(validationService.sanitizeString(name));
        }
        // Actualizar precio si se proporciona
        if (price != null) {
            validationService.validatePrice(price);
            product.setPrice(price);
        }
        // Actualizar descripción
        if (description != null) {
            product.setDescription(description.trim().isEmpty() ?
                    null : validationService.sanitizeString(description));
        }
        // Actualizar imagen
        if (imageUrl != null) {
            product.setImageUrl(imageUrl.trim().isEmpty() ? null : imageUrl.trim());
        }
        return repo.save(product);
    }
    /**
     * Aumentar stock a un producto existente
     */
    @Transactional
    public Product addStock(Long id, Integer quantity) {
        validationService.validateId(id);
        validationService.validatePositiveQuantity(quantity, "Cantidad a agregar");
        Product product = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        product.setStock(currentStock + quantity);
        return repo.save(product);
    }
    /**
     * Reducir stock de un producto (para ventas)
     */
    @Transactional
    public Product reduceStock(Long id, Integer quantity) {
        validationService.validateId(id);
        validationService.validatePositiveQuantity(quantity, "Cantidad a reducir");
        Product product = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        if (currentStock < quantity) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + currentStock
                    + ", Solicitado: " + quantity);
        }
        product.setStock(currentStock - quantity);
        return repo.save(product);
    }
    /**
     * Verificar disponibilidad de stock
     */
    public boolean hasStock(Long id, Integer quantity) {
        validationService.validateId(id);
        validationService.validatePositiveQuantity(quantity, "Cantidad");
        Product product = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        return currentStock >= quantity;
    }
    /**
     * Eliminar producto
     */
    @Transactional
    public void deleteProduct(Long id) {
        validationService.validateId(id);
        if (!repo.existsById(id)) {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
        repo.deleteById(id);
    }
    /**
     * Obtener productos con bajo stock (menor o igual a un umbral)
     */
    public List<Product> getLowStockProducts(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("El umbral debe ser mayor o igual a 0");
        }
        return repo.findAll().stream()
                .filter(product -> {
                    int stock = product.getStock() != null ? product.getStock() : 0;
                    return stock <= threshold;
                })
                .collect(Collectors.toList());
    }
    /**
     * Obtener productos sin stock
     */
    public List<Product> getOutOfStockProducts() {
        return getLowStockProducts(0);
    }
    /**
     * Contar productos totales
     */
    public long countProducts() {
        return repo.count();
    }
    /**
     * Verificar si un producto existe
     */
    public boolean productExists(Long id){
        if (id == null || id <= 0) {
            return false;
        }
        return repo.existsById(id);
    }
}
