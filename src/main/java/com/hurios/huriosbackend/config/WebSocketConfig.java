package com.hurios.huriosbackend.config;

import com.hurios.huriosbackend.websocket.RealtimeNotificationHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración de WebSockets para notificaciones y actualizaciones en tiempo real.
 * Expone el endpoint ws(s)://<host>/ws/events.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeNotificationHandler realtimeNotificationHandler;

    public WebSocketConfig(RealtimeNotificationHandler realtimeNotificationHandler) {
        this.realtimeNotificationHandler = realtimeNotificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeNotificationHandler, "/ws/events")
                .setAllowedOriginPatterns("*");
    }
}
