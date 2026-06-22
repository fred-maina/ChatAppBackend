package com.fredmaina.chatapp.core.models;

import com.fredmaina.chatapp.Auth.Models.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "reported_messages")
public class ReportedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "message_id", nullable = true)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false)
    private String anonymousSessionId;

    @Column(nullable = false)
    private String reason;

    // Snapshot of the reported message at report time. The live ChatMessage row
    // can be deleted later (chat deletion is a normal user action), but the report
    // must keep standing as moderation evidence, so it doesn't rely on `message` surviving.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reportedContent;

    private String reportedNickname;

    private Instant reportedMessageTimestamp;

    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
