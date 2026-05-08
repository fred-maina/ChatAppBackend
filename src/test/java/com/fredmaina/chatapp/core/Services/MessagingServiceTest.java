package com.fredmaina.chatapp.core.Services;

import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessagingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private MessagingService messagingService;

    private User testUser;
    private UUID testUserId;
    private String testSessionId;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        testUserId = UUID.randomUUID();
        testSessionId = "test-session-123";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    void setMessageAsRead_shouldMarkMessagesForUserAndSession() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        messagingService.setMessageAsRead("test@example.com", testSessionId);

        // Assert
        verify(userRepository).findByEmail("test@example.com");
        verify(chatMessageRepository).markMessagesAsRead(testUserId, testSessionId);
    }

    @Test
    void setMessageAsRead_shouldThrowExceptionForUnknownUser() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                messagingService.setMessageAsRead("unknown@example.com", testSessionId)
        );

        assertEquals("User not found: unknown@example.com", exception.getMessage());
        verify(chatMessageRepository, never()).markMessagesAsRead(any(UUID.class), anyString());
    }

    @Test
    void setMessageAsRead_shouldUseCorrectSessionId() {
        // Arrange
        String differentSessionId = "different-session-456";
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        messagingService.setMessageAsRead("test@example.com", differentSessionId);

        // Assert
        verify(chatMessageRepository).markMessagesAsRead(testUserId, differentSessionId);
    }
}
