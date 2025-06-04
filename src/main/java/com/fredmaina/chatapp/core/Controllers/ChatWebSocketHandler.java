package com.fredmaina.chatapp.core.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredmaina.chatapp.Auth.services.JWTService;
import com.fredmaina.chatapp.core.DTOs.WebSocketMessagePayload;
import com.fredmaina.chatapp.core.Services.MessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessagingService messagingService;

    @Autowired
    private JWTService jwtService;

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> anonymousSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String email = extractUsernameFromJWT(session);
        String anonId = extractAnonSessionId(session);

        if (email != null) {
            userSessions.put(email, session);
            log.info("Authenticated user connected: {}", email);
        } else if (anonId != null) {
            anonymousSessions.put(anonId, session);
            log.info("Anonymous user connected: {}", anonId);
        } else {
            log.warn("Connection rejected: No valid token or session cookie.");
            try {
                session.close();
            } catch (Exception ignored) {}
            return;
        }
        List<String> cookies = session.getHandshakeHeaders().get("cookie");
        if (cookies != null) {
            for (String header : cookies) {
                String[] parts = header.split(";");
                for (String part : parts) {
                    String[] keyValue = part.trim().split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("anonSessionId")) {
                        log.info("extracted from cookies");
                    }
                }
            }
        }

        messagingService.getUserSessions().putAll(userSessions);
        messagingService.getAnonymousSessions().putAll(anonymousSessions);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.values().remove(session);
        anonymousSessions.values().remove(session);
        log.info("Connection closed: {}, reason: {}", session.getId(),status.getReason());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebSocketMessagePayload payload = objectMapper.readValue(message.getPayload(), WebSocketMessagePayload.class);

        switch (payload.getType()) {
            case ANON_TO_USER -> {
                String anonId = extractAnonSessionId(session);
                if (anonId != null) {
                    log.info(payload.toString());
                    messagingService.sendMessageFromAnonymous(anonId, payload);
                }
            }
            case USER_TO_ANON -> {
                String username = extractUsernameFromJWT(session);
                if (username != null) {
                    log.info(payload.toString());
                    messagingService.sendMessageFromUser(username, payload.getTo(), payload.getContent());
                }
            }
            case MARK_AS_READ -> {
                messagingService.setMessageAsRead(payload.getChatId());
                log.info(payload.toString());
            }
            default -> log.warn("Unsupported message type: {}", payload.getType());
        }
    }

    // Helper to extract JWT from query params
    private String extractUsernameFromJWT(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                String[] pairs = uri.getQuery().split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("token")) {
                        String token = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        return jwtService.getUsernameFromToken(token);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("JWT extraction failed: {}", e.getMessage());
        }
        return null;
    }

    // Helper to extract session ID from cookies
    private String extractAnonSessionId(WebSocketSession session) {
        try {
            // Try to get from cookies
            List<String> cookies = session.getHandshakeHeaders().get("cookie");
            if (cookies != null) {
                for (String header : cookies) {
                    String[] parts = header.split(";");
                    for (String part : parts) {
                        String[] keyValue = part.trim().split("=");
                        if (keyValue.length == 2 && keyValue[0].equals("anonSessionId")) {
                            log.info("extracted form cookies");
                            return keyValue[1];
                        }
                    }
                }
            }
            // Fallback: Try to get from URI query param
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                String[] queryParams = uri.getQuery().split("&");
                for (String param : queryParams) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("anonSessionId")) {
                        log.info("extracted from  URI");
                        return keyValue[1];
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Anon session extraction failed: {}", e.getMessage());
        }
        return null;
    }

}
