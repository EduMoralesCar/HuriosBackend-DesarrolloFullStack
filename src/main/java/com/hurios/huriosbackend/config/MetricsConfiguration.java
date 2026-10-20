package com.hurios.huriosbackend.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuración de métricas personalizadas para Prometheus
 * 
 * Esta clase proporciona beans para crear métricas personalizadas
 * que pueden ser monitoreadas en Grafana a través de Prometheus.
 * 
 * Ejemplos de uso:
 * - Contar eventos específicos del negocio (pedidos, pagos, etc.)
 * - Medir tiempos de operaciones críticas
 * - Monitorear tasas de error por tipo
 */
@Configuration
public class MetricsConfiguration {

    /**
     * Componente helper para registrar métricas personalizadas
     */
    @Component
    public static class MetricsHelper {
        
        private final MeterRegistry meterRegistry;
        
        // Contadores de eventos de negocio
        private final Counter orderCreatedCounter;
        private final Counter orderCompletedCounter;
        private final Counter orderCancelledCounter;
        private final Counter paymentSuccessCounter;
        private final Counter paymentFailureCounter;
        
        // Temporizadores para operaciones
        private final Timer orderProcessingTimer;
        private final Timer paymentProcessingTimer;

        public MetricsHelper(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Inicializar contadores
            this.orderCreatedCounter = Counter.builder("hurios.orders.created")
                    .description("Total de pedidos creados")
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
                    
            this.orderCompletedCounter = Counter.builder("hurios.orders.completed")
                    .description("Total de pedidos completados")
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
                    
            this.orderCancelledCounter = Counter.builder("hurios.orders.cancelled")
                    .description("Total de pedidos cancelados")
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
                    
            this.paymentSuccessCounter = Counter.builder("hurios.payments.success")
                    .description("Total de pagos exitosos")
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
                    
            this.paymentFailureCounter = Counter.builder("hurios.payments.failure")
                    .description("Total de pagos fallidos")
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
            
            // Inicializar temporizadores
            this.orderProcessingTimer = Timer.builder("hurios.orders.processing.time")
                    .description("Tiempo de procesamiento de pedidos")
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
                    
            this.paymentProcessingTimer = Timer.builder("hurios.payments.processing.time")
                    .description("Tiempo de procesamiento de pagos")
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
        }
        
        // Métodos para incrementar contadores
        
        public void incrementOrderCreated() {
            orderCreatedCounter.increment();
        }
        
        public void incrementOrderCompleted() {
            orderCompletedCounter.increment();
        }
        
        public void incrementOrderCancelled() {
            orderCancelledCounter.increment();
        }
        
        public void incrementPaymentSuccess() {
            paymentSuccessCounter.increment();
        }
        
        public void incrementPaymentFailure() {
            paymentFailureCounter.increment();
        }
        
        // Métodos para temporizadores
        
        public Timer getOrderProcessingTimer() {
            return orderProcessingTimer;
        }
        
        public Timer getPaymentProcessingTimer() {
            return paymentProcessingTimer;
        }
        
        /**
         * Crea un contador personalizado con etiquetas
         * 
         * @param name Nombre de la métrica
         * @param description Descripción de la métrica
         * @param tags Etiquetas adicionales (pares clave-valor)
         * @return Counter
         */
        public Counter createCounter(String name, String description, String... tags) {
            return Counter.builder(name)
                    .description(description)
                    .tags(tags)
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
        }
        
        /**
         * Crea un temporizador personalizado
         * 
         * @param name Nombre del temporizador
         * @param description Descripción
         * @param tags Etiquetas adicionales (pares clave-valor)
         * @return Timer
         */
        public Timer createTimer(String name, String description, String... tags) {
            return Timer.builder(name)
                    .description(description)
                    .tags(tags)
                    .tag("application", "huriosbackend")
                    .register(meterRegistry);
        }
    }
}
