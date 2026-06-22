package com.fredmaina.chatapp.core.Services;

import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private ChatService chatService;

    @Test
    void deleteChatSessionDeletesMessagesInBothDirections() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cacheManager.getCache("chatSessions")).thenReturn(cache);

        chatService.deleteChatSession(userId, "anon-123");

        verify(chatMessageRepository).deleteByFromSessionIdAndToUserId("anon-123", userId);
        verify(chatMessageRepository).deleteByToSessionIdAndFromUserId("anon-123", userId);
        verify(cache).evict(userId);
    }

    @Test
    void markChatSessionAsReadScopesUpdateToUserAndEvictsChatCache() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatMessageRepository.markMessagesAsRead(userId, "anon-123")).thenReturn(3);
        when(cacheManager.getCache("chatSessions")).thenReturn(cache);

        int updatedCount = chatService.markChatSessionAsRead(userId, "anon-123");

        verify(chatMessageRepository).markMessagesAsRead(userId, "anon-123");
        verify(cache).evict(userId);
        org.junit.jupiter.api.Assertions.assertEquals(3, updatedCount);
    }

    @Test
    void getChatHistoryForAnonymousDoesNotMarkRegisteredUserMessagesAsRead() {
        UUID userId = UUID.randomUUID();
        User recipient = new User();
        recipient.setId(userId);
        recipient.setUsername("owner");

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(recipient));
        when(chatMessageRepository.findFullChatHistory("anon-123", userId)).thenReturn(List.of());

        chatService.getChatHistoryForAnonymous("anon-123", "owner");

        verify(chatMessageRepository).findFullChatHistory("anon-123", userId);
        verify(chatMessageRepository, never()).markMessagesAsRead(userId, "anon-123");
    }
}
