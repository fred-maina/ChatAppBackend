package com.fredmaina.chatapp.core.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.DTOs.MessageType;
import com.fredmaina.chatapp.core.DTOs.WebSocketMessagePayload;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import com.fredmaina.chatapp.core.models.ChatMessage;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MessagingService {

    @Getter
    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, WebSocketSession> anonymousSessions = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void sendMessageFromAnonymous(String sessionId, WebSocketMessagePayload dto) {
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            log.warn("Empty message from anonymous user, sessionId={}", sessionId);
            return;
        }

        User toUser = userRepository.findByUsername(dto.getTo())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found"));

        ChatMessage message = ChatMessage.builder()
                .content(dto.getContent())
                .fromSessionId(sessionId)
                .nickname(dto.getNickname())
                .toUser(toUser)
                .timestamp(Instant.now())
                .build();

        chatMessageRepository.save(message);

        log.info("Saved anon → user msg: {} → {}", sessionId, toUser.getUsername());

        WebSocketMessagePayload payload = new WebSocketMessagePayload(
                MessageType.ANON_TO_USER,
                sessionId,
                toUser.getUsername(),
                dto.getContent(),
                dto.getNickname(),
                DateTimeFormatter.ISO_INSTANT.format(message.getTimestamp())
        );

        try {
            redisTemplate.convertAndSend("chat-messages", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to publish message to Redis", e);
        }
    }

    public void sendMessageFromUser(String username, String targetSessionId, String content) {
        if (content == null || content.isBlank()) {
            log.warn("Empty message from user {}", username);
            return;
        }

        User fromUser = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatMessage message = ChatMessage.builder()
                .content(content)
                .fromUser(fromUser)
                .toSessionId(targetSessionId)
                .timestamp(Instant.now())
                .build();

        chatMessageRepository.save(message);

        log.info("Saved user → anon msg: {} → {}", username, targetSessionId);

        WebSocketMessagePayload payload = new WebSocketMessagePayload(
                MessageType.USER_TO_ANON,
                username,
                targetSessionId,
                content,
                null,
                DateTimeFormatter.ISO_INSTANT.format(message.getTimestamp())
        );

        try {
            redisTemplate.convertAndSend("chat-messages", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to publish message to Redis", e);
        }
    }

    @Transactional
    public void setMessageAsRead(String sessionId) {
        log.info("Marking messages as read for session {}", sessionId);
        chatMessageRepository.markMessagesAsRead(sessionId);
    }

    // For RedisSubscriber to call
    public void deliverToSession(String to, String json, boolean isUser) {
        WebSocketSession session = isUser
                ? userSessions.get(to)
                : anonymousSessions.get(to);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("Failed to deliver message to {} session: {}", to, e.getMessage());
            }
        } else {
            log.warn("No active WebSocket session for {}", to);
        }
    }
}
