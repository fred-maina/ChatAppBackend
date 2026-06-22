package com.fredmaina.chatapp.core.Services;

import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.DTOs.BlockRequestDto;
import com.fredmaina.chatapp.core.DTOs.ReportRequestDto;
import com.fredmaina.chatapp.core.Repositories.BlockedSessionRepository;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import com.fredmaina.chatapp.core.Repositories.ReportedMessageRepository;
import com.fredmaina.chatapp.core.models.BlockedSession;
import com.fredmaina.chatapp.core.models.ChatMessage;
import com.fredmaina.chatapp.core.models.ReportedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ReportedMessageRepository reportedMessageRepository;

    @Mock
    private BlockedSessionRepository blockedSessionRepository;

    @InjectMocks
    private ModerationService moderationService;

    @Test
    void reportMessageSavesPendingReportForOwnedAnonymousMessage() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        User reporter = user(userId, "owner@example.com", "owner");
        ChatMessage message = ChatMessage.builder()
                .messageId(messageId)
                .fromSessionId("anon-123")
                .toUser(reporter)
                .content("abusive content")
                .build();

        ReportRequestDto request = new ReportRequestDto();
        request.setMessageId(messageId.toString());
        request.setReason("HARASSMENT");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(reporter));
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(reportedMessageRepository.save(any(ReportedMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportedMessage saved = moderationService.reportMessage("owner@example.com", request);

        assertSame(message, saved.getMessage());
        assertSame(reporter, saved.getReporter());
        assertEquals("anon-123", saved.getAnonymousSessionId());
        assertEquals("HARASSMENT", saved.getReason());
        assertEquals("abusive content", saved.getReportedContent());
        assertEquals("PENDING", saved.getStatus());
        verify(reportedMessageRepository).save(any(ReportedMessage.class));
    }

    @Test
    void reportMessageSnapshotsContentSoItSurvivesMessageDeletion() {
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        User reporter = user(userId, "owner@example.com", "owner");
        ChatMessage message = ChatMessage.builder()
                .messageId(messageId)
                .fromSessionId("anon-123")
                .toUser(reporter)
                .content("abusive content")
                .nickname("Troll")
                .build();

        ReportRequestDto request = new ReportRequestDto();
        request.setMessageId(messageId.toString());
        request.setReason("HARASSMENT");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(reporter));
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        ArgumentCaptor<ReportedMessage> captor = ArgumentCaptor.forClass(ReportedMessage.class);
        when(reportedMessageRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        moderationService.reportMessage("owner@example.com", request);

        ReportedMessage saved = captor.getValue();
        // These fields must not depend on `message` staying attached, since the
        // underlying ChatMessage row can later be deleted by the user's own chat-deletion action.
        assertEquals("abusive content", saved.getReportedContent());
        assertEquals("Troll", saved.getReportedNickname());
    }

    @Test
    void reportMessageRejectsMessageNotSentToReporter() {
        UUID messageId = UUID.randomUUID();
        User reporter = user(UUID.randomUUID(), "owner@example.com", "owner");
        User otherUser = user(UUID.randomUUID(), "other@example.com", "other");
        ChatMessage message = ChatMessage.builder()
                .messageId(messageId)
                .fromSessionId("anon-123")
                .toUser(otherUser)
                .content("content")
                .build();

        ReportRequestDto request = new ReportRequestDto();
        request.setMessageId(messageId.toString());
        request.setReason("SPAM");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(reporter));
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> moderationService.reportMessage("owner@example.com", request)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(reportedMessageRepository, never()).save(any());
    }

    @Test
    void blockSessionSavesBlockRecord() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "owner@example.com", "owner");
        BlockRequestDto request = new BlockRequestDto();
        request.setAnonymousSessionId("anon-123");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(blockedSessionRepository.findByUserIdAndBlockedSessionId(userId, "anon-123")).thenReturn(Optional.empty());
        when(blockedSessionRepository.saveAndFlush(any(BlockedSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BlockedSession blockedSession = moderationService.blockSession("owner@example.com", request);

        assertSame(user, blockedSession.getUser());
        assertEquals("anon-123", blockedSession.getBlockedSessionId());

        ArgumentCaptor<BlockedSession> captor = ArgumentCaptor.forClass(BlockedSession.class);
        verify(blockedSessionRepository).saveAndFlush(captor.capture());
        assertEquals("anon-123", captor.getValue().getBlockedSessionId());
    }

    @Test
    void blockSessionFallsBackToExistingRecordOnConcurrentInsertRace() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "owner@example.com", "owner");
        BlockRequestDto request = new BlockRequestDto();
        request.setAnonymousSessionId("anon-123");

        BlockedSession winningInsert = new BlockedSession();
        winningInsert.setUser(user);
        winningInsert.setBlockedSessionId("anon-123");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(blockedSessionRepository.findByUserIdAndBlockedSessionId(userId, "anon-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winningInsert));
        when(blockedSessionRepository.saveAndFlush(any(BlockedSession.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

        BlockedSession result = moderationService.blockSession("owner@example.com", request);

        assertSame(winningInsert, result);
    }

    @Test
    void acceptEulaUpdatesUserTimestamp() {
        User user = user(UUID.randomUUID(), "owner@example.com", "owner");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = moderationService.acceptEula("owner@example.com");

        assertNotNull(saved.getEulaAcceptedAt());
        verify(userRepository).save(user);
    }

    private User user(UUID id, String email, String username) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(username);
        user.setVerified(true);
        return user;
    }
}
