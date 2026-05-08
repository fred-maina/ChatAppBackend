package com.fredmaina.chatapp.core.Services;

import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.DTOs.ChatMessageDto;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import com.fredmaina.chatapp.core.models.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

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
    void getChatHistoryForAnonymous_shouldNotMarkMessagesAsRead() {
        // Arrange
        ChatMessage message = ChatMessage.builder()
                .messageId(UUID.randomUUID())
                .content("Hello")
                .fromSessionId(testSessionId)
                .toUser(testUser)
                .nickname("Anonymous")
                .isRead(false)
                .timestamp(Instant.now())
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatMessageRepository.findFullChatHistory(testSessionId, testUserId)).thenReturn(List.of(message));

        // Act
        List<ChatMessageDto> result = chatService.getChatHistoryForAnonymous(testSessionId, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        // Verify that markMessagesAsRead was NOT called - this is the key assertion
        verify(chatMessageRepository, never()).markMessagesAsRead(any(UUID.class), anyString());
    }

    @Test
    void getChatHistoryForAnonymous_shouldReturnMessagesUnchanged() {
        // Arrange
        ChatMessage message1 = ChatMessage.builder()
                .messageId(UUID.randomUUID())
                .content("Hello")
                .fromSessionId(testSessionId)
                .toUser(testUser)
                .nickname("Anonymous")
                .isRead(false)
                .timestamp(Instant.now().minusSeconds(60))
                .build();

        ChatMessage message2 = ChatMessage.builder()
                .messageId(UUID.randomUUID())
                .content("How are you?")
                .fromSessionId(testSessionId)
                .toUser(testUser)
                .nickname("Anonymous")
                .isRead(false)
                .timestamp(Instant.now())
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatMessageRepository.findFullChatHistory(testSessionId, testUserId)).thenReturn(List.of(message1, message2));

        // Act
        List<ChatMessageDto> result = chatService.getChatHistoryForAnonymous(testSessionId, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Hello", result.get(0).getText());
        assertEquals("How are you?", result.get(1).getText());
    }

    @Test
    void getChatHistoryForAnonymous_shouldThrowExceptionForUnknownUser() {
        // Arrange
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                chatService.getChatHistoryForAnonymous(testSessionId, "unknownuser")
        );

        assertEquals("Recipient user not found: unknownuser", exception.getMessage());
    }
}
