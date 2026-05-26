package com.fredmaina.chatapp.core.DTOs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Builder
@Getter
@Setter
public class ChatSessionDto {
    private String id;
    private String senderNickname;
    private String lastMessage;
    private Instant lastMessageTimestamp;
    private int unreadCount;
    private String avatarUrl;
    private List<ChatMessageDto> messages;
}