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

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final MessagingService messagingService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            WebSocketMessagePayload payload = objectMapper.readValue(json, WebSocketMessagePayload.class);

            log.info("Received message from Redis: {}", payload);

            if (payload.getType().name().equals("ANON_TO_USER")) {
                WebSocketSession session = messagingService.getUserSessions().get(payload.getTo());
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
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
