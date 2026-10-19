# 📊 Ejemplo de Uso de Métricas Personalizadas

Este documento muestra cómo usar las métricas personalizadas de Prometheus en los controladores y servicios de Spring Boot.

## 📝 Importar MetricsHelper

```java
import com.hurios.huriosbackend.config.MetricsConfiguration.MetricsHelper;
import org.springframework.beans.factory.annotation.Autowired;
```

## 🔢 Ejemplo 1: Contador Simple

Para contar eventos (ej. pedidos creados):

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private MetricsHelper metricsHelper;
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Order order) {
        // Lógica del negocio...
        orderService.create(order);
        
        // Incrementar contador de pedidos creados
        metricsHelper.incrementOrderCreated();
        
        return ResponseEntity.ok(order);
    }
}
```

## ⏱️ Ejemplo 2: Medir Tiempo de Ejecución

Para medir cuánto tarda una operación:

```java
@Service
public class PaymentService {

    @Autowired
    private MetricsHelper metricsHelper;
    
    public void processPayment(Payment payment) {
        // Registrar el tiempo de procesamiento
        metricsHelper.getPaymentProcessingTimer().record(() -> {
            // Lógica de procesamiento de pago
            // ...
            if (paymentSuccessful) {
                metricsHelper.incrementPaymentSuccess();
            } else {
                metricsHelper.incrementPaymentFailure();
            }
        });
    }
}
```

## 🏷️ Ejemplo 3: Métricas con Etiquetas Personalizadas

Crear métricas específicas para diferentes categorías:

```java
@Service
public class ProductService {

    @Autowired
    private MetricsHelper metricsHelper;
    
    private Counter productViewCounter;
    
    @PostConstruct
    public void init() {
        // Crear contador personalizado
        productViewCounter = metricsHelper.createCounter(
            "hurios.products.views",
            "Visualizaciones de productos por categoría",
            "category", "all"
        );
    }
    
    public Product getProduct(Long id, String category) {
        Product product = productRepository.findById(id);
        
        // Incrementar con etiqueta de categoría
        metricsHelper.createCounter(
            "hurios.products.views",
            "Visualizaciones de productos",
            "category", category
        ).increment();
        
        return product;
    }
}
```

## 📊 Ejemplo 4: Temporizador Manual

Para medir operaciones que no se pueden envolver en un lambda:

```java
@Service
public class OrderService {

    @Autowired
    private MetricsHelper metricsHelper;
    
    public void processOrder(Order order) {
        Timer.Sample sample = Timer.start();
        
        try {
            // Procesar pedido
            validateOrder(order);
            calculateTotal(order);
            saveOrder(order);
            
            metricsHelper.incrementOrderCreated();
            
        } finally {
            // Registrar el tiempo transcurrido
            sample.stop(metricsHelper.getOrderProcessingTimer());
        }
    }
}
```

## 🎯 Ejemplo 5: Métricas Condicionales

Registrar diferentes métricas según el resultado:

```java
@Service
public class OrderService {

    @Autowired
    private MetricsHelper metricsHelper;
    
    public void updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId);
        order.setStatus(newStatus);
        orderRepository.save(order);
        
        // Incrementar contadores según el estado
        switch (newStatus) {
            case "COMPLETED":
                metricsHelper.incrementOrderCompleted();
                break;
            case "CANCELLED":
                metricsHelper.incrementOrderCancelled();
                break;
        }
    }
}
```

## 📈 Consultas en Prometheus/Grafana

Una vez implementadas las métricas, puedes consultarlas en Prometheus:

### Tasa de pedidos creados por minuto:
```promql
rate(hurios_orders_created_total[1m])
```

### Total de pedidos creados:
```promql
hurios_orders_created_total
```

### Tiempo promedio de procesamiento de pagos:
```promql
rate(hurios_payments_processing_time_sum[5m]) / rate(hurios_payments_processing_time_count[5m])
```

### Tasa de éxito de pagos:
```promql
hurios_payments_success_total / (hurios_payments_success_total + hurios_payments_failure_total)
```

### Percentil 95 del tiempo de procesamiento de pedidos:
```promql
histogram_quantile(0.95, rate(hurios_orders_processing_time_bucket[5m]))
```

## 🎨 Añadir al Dashboard de Grafana

Para añadir estas métricas al dashboard:

1. Abre Grafana en `http://localhost:3000`
2. Ve al dashboard "Hurios Rally - Spring Boot Metrics"
3. Haz clic en "Add Panel"
4. Añade una consulta PromQL de las anteriores
5. Configura el tipo de visualización (Graph, Gauge, Stat, etc.)
6. Guarda el panel

## 📚 Recursos

- [Documentación de Micrometer](https://micrometer.io/docs)
- [PromQL Documentation](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Panels](https://grafana.com/docs/grafana/latest/panels/)
