package com.hurios.huriosbackend.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * CacheService - Servicio de caché usando Google Guava
 * Mejora el rendimiento al cachear datos frecuentemente consultados
 */
@Service
public class CacheService {
    private final LoadingCache<Long, Product> productCache;
    private final LoadingCache<String, User> userByEmailCache;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CacheService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productCache = CacheBuilder.newBuilder().maximumSize(100)  // Máximo 100 productos en cache
                .expireAfterWrite(10, TimeUnit.MINUTES)  // Expira después de 10 minutos
                .recordStats()  // Registrar estadísticas
                .build(new CacheLoader<Long, Product>() {
                    @Override
                    public Product load(Long id) throws Exception {
                        return productRepository.findById(id)
                                .orElseThrow(() -> new Exception("Producto no encontrado: " + id));
                    }
                });
        // Cache de usuarios por email - expira después de 15 minutos
        this.userByEmailCache = CacheBuilder.newBuilder()
                .maximumSize(50)  // Máximo 50 usuarios en cache
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build(new CacheLoader<String, User>() {
                    @Override
                    public User load(String email) throws Exception {
                        return userRepository.findByEmail(email)
                                .orElseThrow(() -> new Exception("Usuario no encontrado: " + email));
                    }
                });
    }
    /**
     * Sirve para obtener producto desde cache (o cargar si no existe)
     */
    public Product getProduct(Long id) throws ExecutionException {
        return productCache.get(id);
    }
    /**
     * Obtener usuario por email desde cache
     */
    public User getUserByEmail(String email) throws ExecutionException {
        return userByEmailCache.get(email);
    }
    /**
     * Invalidar producto del cache (después de actualización)
     */
    public void invalidateProduct(Long id) {
        productCache.invalidate(id);
    }
    /**
     * Invalidar usuario del cache
     */
    public void invalidateUser(String email) {
        userByEmailCache.invalidate(email);
    }
    /**
     * Limpiar todo el cache de productos
     * */
    public void clearProductCache() {
        productCache.invalidateAll();
    }
    /**
     * Limpiar todo el cache de usuarios
     */
    public void clearUserCache() {
        userByEmailCache.invalidateAll();
    }
    /**
     * Obtener estadísticas del cache de productos
     */
    public String getProductCacheStats() {
        return "Product Cache Stats: " + productCache.stats().toString();
    }
    /**
     * Obtener estadísticas del cache de usuarios
     */
    public String getUserCacheStats() {
        return "User Cache Stats: " + userByEmailCache.stats().toString();
    }
    /**
     * Precalentar cache con productos más populares
     */
    public void warmUpProductCache() {
        // Cargar los primeros 20 productos en cache
        productRepository.findAll().stream()
                .limit(20)
                .forEach(product -> productCache.put(product.getId(), product));
    }
}
