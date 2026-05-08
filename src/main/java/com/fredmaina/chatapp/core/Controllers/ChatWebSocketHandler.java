package com.fredmaina.chatapp.core.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.services.WebSocketTicketService;
import com.fredmaina.chatapp.core.DTOs.WebSocketMessagePayload;
import com.fredmaina.chatapp.core.Services.MessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final WebSocketTicketService webSocketTicketService;

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> anonymousSessions = new ConcurrentHashMap<>();

    // Store the authenticated email for each session (for message handling)
    private final Map<String, String> sessionEmailMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String email = extractUserFromTicket(session);
        String anonId = extractAnonSessionId(session);
        Optional<String> userName = email != null 
                ? userRepository.findByEmail(email).map(User::getUsername)
                : Optional.empty();

        if (userName.isPresent()) {
            String user = userName.get();
            userSessions.put(user, session);
            sessionEmailMap.put(session.getId(), email);
            log.info("Authenticated user connected: {}, with nickname: {}", email, user);
        } else if (anonId != null) {
            anonymousSessions.put(anonId, session);
            log.info("Anonymous user connected: {}", anonId);
        } else {
            log.warn("Connection rejected: No valid ticket or session cookie.");
            try {
                session.close();
            } catch (Exception ignored) {}
            return;
        }
        List<String> cookies = session.getHandshakeHeaders().get("cookie");
        if (cookies != null) {
            for (String header : cookies) {
                log.info(header);
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
        sessionEmailMap.remove(session.getId());
        log.info("Connection closed: {}, reason: {}", session.getId(), status.getReason());
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
                String email = sessionEmailMap.get(session.getId());
                if (email != null) {
                    log.info(payload.toString());
                    messagingService.sendMessageFromUser(email, payload.getTo(), payload.getContent());
                }
            }
            case MARK_AS_READ -> {
                messagingService.setMessageAsRead(payload.getChatId());
                log.info(payload.toString());
            }
            default -> log.warn("Unsupported message type: {}", payload.getType());
        }
    }

    /**
     * Extracts user email from a one-time-use WebSocket ticket.
     * The ticket is validated and immediately consumed (evicted from cache)
     * to prevent reuse.
     */
    private String extractUserFromTicket(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                String[] pairs = uri.getQuery().split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("ticket")) {
                        String ticket = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        return webSocketTicketService.validateAndConsumeTicket(ticket);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Ticket extraction failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extracts anonymous session ID from cookies only.
     * Query parameter fallback has been removed for security reasons.
     */
    private String extractAnonSessionId(WebSocketSession session) {
        try {
            // Get from cookies only (query param fallback removed for security)
            List<String> cookies = session.getHandshakeHeaders().get("cookie");
            if (cookies != null) {
                for (String header : cookies) {
                    String[] parts = header.split(";");
                    for (String part : parts) {
                        String[] keyValue = part.trim().split("=");
                        if (keyValue.length == 2 && keyValue[0].equals("anonSessionId")) {
                            log.info("extracted from cookies");
                            return keyValue[1];
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Anon session extraction failed: {}", e.getMessage());
        }
        return null;
    }

}
