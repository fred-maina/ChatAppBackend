package com.fredmaina.chatapp.core.models;

import com.fredmaina.chatapp.Auth.Models.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant; // Changed from LocalDateTime
import java.util.UUID;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID messageId;

    private String content;

    private String fromSessionId;  // nullable: only set if it's from anonymous

    private String toSessionId;    // nullable: only set if it's to anonymous

    private String nickname;       // optional: only needed if from anonymous

    private Instant timestamp; // Changed to Instant

    @ManyToOne
    @JoinColumn(name = "from_user_id")
    private User fromUser;         // nullable: only set if it's from a user

    @ManyToOne
    @JoinColumn(name = "to_user_id")
    private User toUser;           // nullable: only set if it's to a user

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isRead = false; // New field, defaults to false
}