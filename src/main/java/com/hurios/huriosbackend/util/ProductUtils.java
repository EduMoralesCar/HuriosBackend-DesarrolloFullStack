package com.hurios.huriosbackend.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.hurios.huriosbackend.entity.Product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductUtils {
    // Categorías disponibles (inmutable)
    public static final ImmutableList<String> CATEGORIES = ImmutableList.of(
            "Motor", "Suspensión", "Frenos", "Eléctrico", "Transmisión",
            "Carrocería", "Neumáticos", "Accesorios", "Lubricantes", "Filtros"
    );
    // Rangos de precio predefinidos (inmutable)
    public static final ImmutableMap<String, Range<Double>> PRICE_RANGES=ImmutableMap.of(
            "Económico", Range.closedOpen(0.0, 50.0),
            "Medio", Range.closedOpen(50.0, 150.0),
            "Premium", Range.closedOpen(150.0, 500.0),
            "Lujo", Range.atLeast(500.0)
    );
    /**
     * Agrupar productos por categoría usando Multimap
     */
    public static Multimap<String, Product> groupByCategory(List<Product> products) {
        Multimap<String, Product> grouped = ArrayListMultimap.create();
        for (Product product : products) {
            if (product.getName() != null) {
                // Intentar determinar categoría del nombre del producto
                String category = detectCategory(product.getName());
                grouped.put(category, product);
            }
        }
        return grouped;
    }
    /**
     * Detectar categoría basada en palabras clave
     */
    public static String detectCategory(String productName){
        String lowerName = productName.toLowerCase();
        if (lowerName.contains("motor") || lowerName.contains("pistón")) {
            return "Motor";
        } else if (lowerName.contains("freno") || lowerName.contains("pastilla")) {
            return "Frenos";
        } else if (lowerName.contains("suspensión") || lowerName.contains("amortiguador")) {
            return "Suspensión";
        } else if (lowerName.contains("eléctrico") || lowerName.contains("batería")) {
            return "Eléctrico";
        } else if (lowerName.contains("aceite") || lowerName.contains("lubricante")) {
            return "Lubricantes";
        } else if(lowerName.contains("neumático")){
            return "Neumáticos";
        } else if(lowerName.contains("puertas") || lowerName.contains("capó") || lowerName.contains("espejos") || lowerName.contains("techo")) {
            return "Carrocería";
        } else if(lowerName.contains("embrague") || lowerName.contains("caja de cambios") || lowerName.contains("diferencial")){
            return "Transmisión";
        }
        return "Accesorios";
    }
    /**
     * Agrupar productos por rango de precio
     */
    public static Map<String, List<Product>> groupByPriceRange(List<Product> products) {
        Map<String, List<Product>> grouped = new HashMap<>();
        for (Map.Entry<String, Range<Double>> entry : PRICE_RANGES.entrySet()) {
            List<Product> productsInRange = products.stream()
                    .filter(p -> p.getPrice() != null && entry.getValue().contains(p.getPrice()))
                    .collect(Collectors.toList());
            if (!productsInRange.isEmpty()) {
                grouped.put(entry.getKey(), productsInRange);
            }
        }

        return grouped;
    }
    /**
     * Paginar lista de productos
     */
    public static List<List<Product>> paginateProducts(List<Product> products, int pageSize) {
        return Lists.partition(products, pageSize);
    }
    /**
     * Ordenar productos por múltiples criterios
     */
    public static List<Product> sortByMultipleCriteria(List<Product> products) {
        // Ordenar por: 1) Stock (desc), 2) Precio (asc), 3) Nombre (asc)
        Ordering<Product> ordering = Ordering.natural().nullsLast()
                .onResultOf((Product p) -> p.getStock() != null ? -p.getStock() : 0)
                .compound(Ordering.natural().nullsLast().onResultOf(Product::getPrice))
                .compound(Ordering.natural().nullsLast().onResultOf(Product::getName));
        return ordering.immutableSortedCopy(products);
    }
    /**
     * Filtrar productos con stock bajo
     */
    public static ImmutableList<Product> filterLowStock(List<Product> products, int threshold) {
        return ImmutableList.copyOf(
                products.stream()
                        .filter(p -> p.getStock() != null && p.getStock() <= threshold)
                        .collect(Collectors.toList())
        );
    }
    /**
     * Obtener estadísticas de stock por categoría
     */
    public static Map<String, Integer> getStockByCategory(List<Product> products) {
        Multimap<String, Product> byCategory = groupByCategory(products);

        ImmutableMap.Builder<String, Integer> stats = ImmutableMap.builder();
        for (String category : byCategory.keySet()) {
            int totalStock = byCategory.get(category).stream()
                    .mapToInt(p -> p.getStock() != null ? p.getStock() : 0)
                    .sum();
            stats.put(category, totalStock);
        }
        return stats.build();
    }

    /**
     * Crear string de tags/categorías
     */
    public static String joinTags(List<String> tags) {
        return Joiner.on(", ")
                .skipNulls()
                .join(tags);
    }
    /**
     * Parsear tags desde string
     */
    public static List<String> parseTags(String tagsString) {
        if (tagsString == null || tagsString.trim().isEmpty()) {
            return ImmutableList.of();
        }
        return Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .splitToList(tagsString);
    }
    /**
     * Crear tabla de búsqueda rápida por ID
     */
    public static ImmutableMap<Long, Product> createLookupTable(List<Product> products) {
        return Maps.uniqueIndex(products, Product::getId);
    }
    /**
     * Transformar lista de productos a mapa de nombre -> precio
     */
    public static ImmutableMap<String, Double> createPriceMap(List<Product> products) {
        return products.stream().filter(p -> p.getName() != null && p.getPrice() != null)
                .collect(ImmutableMap.toImmutableMap(
                        Product::getName,
                        Product::getPrice,
                        (p1, p2) -> p1 // En caso de duplicados, usar el primero
                ));
    }
    /**
     * Obtener productos más populares (por nombre más corto - simulación)
     */
    public static List<Product> getTopN(List<Product> products, int n) {
        return Ordering.natural()
                .nullsLast()
                .onResultOf((Product p) -> p.getName() != null ? p.getName().length() : Integer.MAX_VALUE)
                .leastOf(products, n);
    }
    /**
     * Combinar listas de productos eliminando duplicados
     */
    public static ImmutableSet<Product> combineUnique(List<Product>... productLists) {
        ImmutableSet.Builder<Product> builder = ImmutableSet.builder();
        for (List<Product> list : productLists) {
            builder.addAll(list);
        }
        return builder.build();
    }
    /**
     * Crear frecuencia de palabras en nombres de productos
     */
    public static Multiset<String> getWordFrequency(List<Product> products) {
        Multiset<String> wordFrequency = HashMultiset.create();
        for (Product product : products) {
            if (product.getName() != null) {
                List<String> words = Splitter.on(' ')
                        .trimResults()
                        .omitEmptyStrings()
                        .splitToList(product.getName().toLowerCase());
                wordFrequency.addAll(words);
            }
        }
        return wordFrequency;
    }
    /**
     * Validar rango de precio
     */
    public static boolean isPriceInRange(Double price, String rangeName) {
        if (price == null || !PRICE_RANGES.containsKey(rangeName)) {
            return false;
        }
        return PRICE_RANGES.get(rangeName).contains(price);
    }
}
