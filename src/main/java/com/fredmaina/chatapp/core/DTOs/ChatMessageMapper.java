package com.fredmaina.chatapp.core.DTOs;

import com.fredmaina.chatapp.core.models.ChatMessage;

import java.util.UUID;
import java.time.format.DateTimeFormatter;
public class ChatMessageMapper {
    public static ChatMessageDto toDto(ChatMessage msg, UUID currentUserId) {
        boolean isFromCurrentUser = msg.getFromUser() != null && msg.getFromUser().getId().equals(currentUserId);

        String formattedTimestamp = msg.getTimestamp() != null ?
                DateTimeFormatter.ISO_INSTANT.format(msg.getTimestamp()) :
                null;

        return ChatMessageDto.builder()
                .id(msg.getMessageId().toString())
                .text(msg.getContent())
                .senderType(isFromCurrentUser ? "self" : "anonymous")
                .timestamp(formattedTimestamp) // Use formattedTimestamp
                .nickname(isFromCurrentUser ? null : msg.getNickname())
                .build();
    }

}