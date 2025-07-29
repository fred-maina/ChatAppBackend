package com.fredmaina.chatapp.core.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredmaina.chatapp.core.DTOs.WebSocketMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final MessagingService messagingService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            WebSocketMessagePayload payload = objectMapper.readValue(json, WebSocketMessagePayload.class);
            if (payload.getType().name().equals("ANON_TO_USER")) {

                WebSocketSession session = messagingService.getUserSessions().get(payload.getTo());
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                } else {
                    log.warn("No active WebSocket session found for user: {}", payload.getTo());
                }
        } else if (payload.getType().name().equals("USER_TO_ANON")) {
                WebSocketSession session = messagingService.getAnonymousSessions().get(payload.getTo());

                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("Error handling Redis pub/sub message", e);
        }
    }
}
