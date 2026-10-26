package com.hurios.huriosbackend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manejador WebSocket bidireccional en tiempo real para Hurios Rally.
 * Permite notificar a todos los clientes conectados sobre actualizaciones de stock,
 * nuevas órdenes de compra, cambios en el catálogo y nuevas reseñas sin recargar la página.
 */
@Component
public class RealtimeNotificationHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotificationHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public RealtimeNotificationHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.debug("Cliente WebSocket conectado: {} (total activos: {})", session.getId(), sessions.size());

        Map<String, Object> welcomePayload = new HashMap<>();
        welcomePayload.put("type", "CONNECTED");
        welcomePayload.put("message", "Conectado al canal en tiempo real de Hurios Rally");
        welcomePayload.put("activeClients", sessions.size());
        welcomePayload.put("timestamp", LocalDateTime.now().toString());

        sendToSession(session, welcomePayload);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> incoming = objectMapper.readValue(payload, Map.class);
            String type = String.valueOf(incoming.getOrDefault("type", "PING"));

            if ("PING".equalsIgnoreCase(type)) {
                Map<String, Object> pong = new HashMap<>();
                pong.put("type", "PONG");
                pong.put("activeClients", sessions.size());
                pong.put("timestamp", LocalDateTime.now().toString());
                sendToSession(session, pong);
            } else {
                String msgText = String.valueOf(incoming.getOrDefault("message", "Evento recibido"));
                broadcastEvent(type, msgText, incoming);
            }
        } catch (Exception e) {
            Map<String, Object> ack = new HashMap<>();
            ack.put("type", "PONG");
            ack.put("activeClients", sessions.size());
            ack.put("timestamp", LocalDateTime.now().toString());
            sendToSession(session, ack);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.debug("Cliente WebSocket desconectado: {} (total activos: {})", session.getId(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * Emite un evento en tiempo real a todos los clientes WebSocket conectados.
     */
    public void broadcastEvent(String type, String message, Map<String, Object> data) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("message", message);
        event.put("data", data != null ? data : Map.of());
        event.put("activeClients", sessions.size());
        event.put("timestamp", LocalDateTime.now().toString());

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.warn("Error serializando evento WebSocket: {}", e.getMessage());
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
    }

    public int getActiveConnectionsCount() {
        return sessions.size();
    }

    private void sendToSession(WebSocketSession session, Map<String, Object> payload) {
        if (!session.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            sessions.remove(session);
        }
    }
}
