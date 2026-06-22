package com.fredmaina.chatapp.core.Controllers;

import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.core.DTOs.BlockRequestDto;
import com.fredmaina.chatapp.core.DTOs.ReportRequestDto;
import com.fredmaina.chatapp.core.Services.ModerationService;
import com.fredmaina.chatapp.core.models.BlockedSession;
import com.fredmaina.chatapp.core.models.ReportedMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/moderation/report")
    public ResponseEntity<?> reportMessage(
            @Valid @RequestBody ReportRequestDto request,
            Authentication authentication) {

        ReportedMessage report = moderationService.reportMessage(authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Message reported successfully.",
                "reportId", report.getId(),
                "status", report.getStatus()
        ));
    }

    @PostMapping("/moderation/block")
    public ResponseEntity<?> blockSession(
            @Valid @RequestBody BlockRequestDto request,
            Authentication authentication) {

        BlockedSession blockedSession = moderationService.blockSession(authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Anonymous session blocked successfully.",
                "blockId", blockedSession.getId(),
                "blockedSessionId", blockedSession.getBlockedSessionId()
        ));
    }

    @PostMapping("/users/accept-eula")
    public ResponseEntity<?> acceptEula(Authentication authentication) {
        User user = moderationService.acceptEula(authentication.getName());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "EULA accepted successfully.",
                "eulaAcceptedAt", user.getEulaAcceptedAt()
        ));
    }
}
