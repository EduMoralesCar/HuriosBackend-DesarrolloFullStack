package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncService.class);

    @Value("${gemini.api-key:}")
    private String geminiApiKey;


    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Lista de imágenes locales disponibles en public/assets/imgs
    private final Map<String, String> categoryImageMap = Map.of(
            "Frenos", "/assets/imgs/corona_con_ruster.webp",
            "Transmisión", "/assets/imgs/corona_con_ruster.webp",
            "Motor", "/assets/imgs/tubo_de_escape.webp",
            "Neumáticos", "/assets/imgs/llanta.webp",
            "Filtros", "/assets/imgs/filtros_de_aire_universal.webp",
            "Carrocería", "/assets/imgs/asiento_carguero.webp",
            "Chasis", "/assets/imgs/CHASIS_BROSS_S_TRAPECIO_EJE_Y_PARADOR_LATERAL.webp",
            "Lubricantes", "/assets/imgs/placeholder.svg"
    );

    public CatalogSyncService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sincroniza el catálogo de repuestos consultando la API de Google Gemini
     * para generar un listado profesional y realista de repuestos para mototaxis (Bajaj, TVS) y motos lineales en Lima.
     */
    public List<Product> syncCatalogFromAI() {
        String prompt = """
            Eres un especialista en inventario de repuestos automotrices para mototaxis (Torito Bajaj, TVS King) y motocicletas lineales en Lima, Perú.
            Genera un JSON EXACTO (un array de objetos, sin bloques markdown como ```json) con 12 repuestos reales de alta rotación para el negocio Hurios Rally E.I.R.L.
            Cada objeto debe tener exactamente estos campos:
            - "name": Nombre comercial completo y técnico (ej. 'Pastillas de Freno TVS King Delanteras')
            - "description": Breve descripción técnica de compatibilidad y materiales (1 o 2 oraciones)
            - "price": Precio sugerido en soles (número flotante, ej. 28.50)
            - "stock": Cantidad en inventario (entero entre 10 y 60)
            - "category": Una de estas categorías: 'Motor', 'Frenos', 'Transmisión', 'Neumáticos', 'Filtros', 'Carrocería', 'Chasis', 'Lubricantes'
            """;

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiApiKey;

        try {
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> requestBody = Map.of("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String rawText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    
                    // Limpiar posibles delimitadores markdown
                    String cleanJson = rawText.replaceAll("(?s)```json\\s*|```", "").trim();
                    JsonNode productsNode = objectMapper.readTree(cleanJson);

                    if (productsNode.isArray()) {
                        List<Product> savedProducts = new ArrayList<>();
                        for (JsonNode item : productsNode) {
                            String name = item.path("name").asText();
                            
                            // Verificar si ya existe para no duplicar
                            boolean exists = productRepository.findAll().stream()
                                    .anyMatch(p -> p.getName().equalsIgnoreCase(name));
                            if (exists) continue;

                            Product p = new Product();
                            p.setName(name);
                            p.setDescription(item.path("description").asText());
                            p.setPrice(item.path("price").asDouble(25.0));
                            p.setStock(item.path("stock").asInt(15));
                            String cat = item.path("category").asText("Motor");
                            p.setCategory(cat);
                            p.setImageUrl(categoryImageMap.getOrDefault(cat, "/assets/imgs/placeholder.svg"));
                            p.setCreatedAt(LocalDateTime.now());

                            savedProducts.add(productRepository.save(p));
                        }
                        log.info("Catálogo sincronizado exitosamente vía API: {} nuevos productos.", savedProducts.size());
                        return savedProducts;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al sincronizar catálogo con la API de Gemini: {}", e.getMessage(), e);
            throw new RuntimeException("Error en sincronización con la API: " + e.getMessage());
        }

        return Collections.emptyList();
    }
}
