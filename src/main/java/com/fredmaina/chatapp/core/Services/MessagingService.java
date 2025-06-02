package com.fredmaina.chatapp.core.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.DTOs.MessageType;
import com.fredmaina.chatapp.core.DTOs.WebSocketMessagePayload;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import com.fredmaina.chatapp.core.models.ChatMessage;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MessagingService {

    // Optional: expose this for WebSocket connection registration
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
                .timestamp(LocalDateTime.now())
                .build();

        chatMessageRepository.save(message);
        log.info("Saved message from anon to user {}: {}", toUser.getUsername(), dto.getContent());

        WebSocketSession receiverSession = userSessions.get(toUser.getEmail());


        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                WebSocketMessagePayload payload = new WebSocketMessagePayload(
                        MessageType.ANON_TO_USER,
                        sessionId,
                        toUser.getUsername(),
                        dto.getContent(),
                        dto.getNickname(),
                        LocalDateTime.now().toString()
                );
                receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException e) {
                log.error("Error sending message to user {}: {}", toUser.getUsername(), e.getMessage());
            }
        }
    }

    public void sendMessageFromUser(String username, String targetSessionId, String content) {
        if (content == null || content.isBlank()) {
            log.warn("Empty message from user {}", username);
            return;
        }

        WebSocketSession session = anonymousSessions.get(targetSessionId);
        if (session != null && session.isOpen()) {
            try {
                WebSocketMessagePayload payload = new WebSocketMessagePayload(
                        MessageType.USER_TO_ANON,
                        username,
                        targetSessionId,
                        content,
                        null,
                        LocalDateTime.now().toString()
                );

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                log.info("Looking up user with username: {}", username);

                ChatMessage message = ChatMessage.builder()
                        .content(content)
                        .fromUser(userRepository.findByUsernameOrEmail(username,username)
                                .orElseThrow(() -> new RuntimeException("User not found")))
                        .toSessionId(targetSessionId)
                        .timestamp(LocalDateTime.now())
                        .build();

                chatMessageRepository.save(message);
                log.info("User {} sent message to anon session {}", username, targetSessionId);

            } catch (IOException e) {
                log.error("Error sending message from {} to anon {}: {}", username, targetSessionId, e.getMessage());
            }
        }
    }


    @Transactional
    public void setMessageAsRead(String sessionId) {
        log.info("Setting messages as read {}", sessionId);
        chatMessageRepository.markMessagesAsRead(sessionId);
    }
}
