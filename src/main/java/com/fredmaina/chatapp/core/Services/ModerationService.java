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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportedMessageRepository reportedMessageRepository;
    private final BlockedSessionRepository blockedSessionRepository;

    @Transactional
    public ReportedMessage reportMessage(String reporterEmail, ReportRequestDto request) {
        User reporter = getUserByEmail(reporterEmail);
        UUID messageId = parseUuid(request.getMessageId(), "messageId must be a valid UUID");

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.getToUser() == null || !reporter.getId().equals(message.getToUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only report messages sent to you");
        }

        if (message.getFromSessionId() == null || message.getFromSessionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only anonymous messages can be reported");
        }

        ReportedMessage report = new ReportedMessage();
        report.setMessage(message);
        report.setReporter(reporter);
        report.setAnonymousSessionId(message.getFromSessionId());
        report.setReason(request.getReason().trim());
        report.setReportedContent(message.getContent());
        report.setReportedNickname(message.getNickname());
        report.setReportedMessageTimestamp(message.getTimestamp());
        report.setStatus("PENDING");

        return reportedMessageRepository.save(report);
    }

    // Not @Transactional: each repository call below must commit/fail in its own
    // transaction so a unique-constraint violation on the save() doesn't poison
    // an outer transaction and break the fallback lookup that follows it.
    public BlockedSession blockSession(String userEmail, BlockRequestDto request) {
        User user = getUserByEmail(userEmail);
        String anonymousSessionId = request.getAnonymousSessionId().trim();

        Optional<BlockedSession> existing = blockedSessionRepository
                .findByUserIdAndBlockedSessionId(user.getId(), anonymousSessionId);
        if (existing.isPresent()) {
            return existing.get();
        }

        BlockedSession blockedSession = new BlockedSession();
        blockedSession.setUser(user);
        blockedSession.setBlockedSessionId(anonymousSessionId);

        try {
            return blockedSessionRepository.saveAndFlush(blockedSession);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent block request for the same pair.
            return blockedSessionRepository
                    .findByUserIdAndBlockedSessionId(user.getId(), anonymousSessionId)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public User acceptEula(String userEmail) {
        User user = getUserByEmail(userEmail);
        user.setEulaAcceptedAt(Instant.now());
        return userRepository.save(user);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UUID parseUuid(String value, String errorMessage) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }
}
