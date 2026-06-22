package com.fredmaina.chatapp.core.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.DTOs.MessageType;
import com.fredmaina.chatapp.core.DTOs.WebSocketMessagePayload;
import com.fredmaina.chatapp.core.Repositories.BlockedSessionRepository;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import com.fredmaina.chatapp.core.models.ChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private BlockedSessionRepository blockedSessionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private MessagingService messagingService;

    @Test
    void sendMessageFromAnonymousRejectsBlockedSessionBeforeSaving() {
        UUID userId = UUID.randomUUID();
        User targetUser = new User();
        targetUser.setId(userId);
        targetUser.setUsername("owner");

        WebSocketMessagePayload payload = new WebSocketMessagePayload();
        payload.setType(MessageType.ANON_TO_USER);
        payload.setTo("owner");
        payload.setContent("hello");
        payload.setNickname("Anon");

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(targetUser));
        when(blockedSessionRepository.existsByUserIdAndBlockedSessionId(userId, "anon-123")).thenReturn(true);

        assertThrows(
                MessageBlockedException.class,
                () -> messagingService.sendMessageFromAnonymous("anon-123", payload)
        );

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }
}
