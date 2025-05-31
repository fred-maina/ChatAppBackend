package com.fredmaina.chatapp.core.DTOs;

import com.fredmaina.chatapp.core.models.ChatMessage;

import java.util.UUID;

public class ChatMessageMapper {
        public static ChatMessageDto toDto(ChatMessage msg, UUID currentUserId) {
            boolean isFromCurrentUser = msg.getFromUser() != null && msg.getFromUser().getId().equals(currentUserId);

            return ChatMessageDto.builder()
                    .id(msg.getMessageId().toString())
                    .text(msg.getContent())
                    .senderType(isFromCurrentUser ? "self" : "anonymous")
                    .timestamp(msg.getTimestamp().toString())
                    .nickname(isFromCurrentUser ? null : msg.getNickname())
                    .build();
        }

}
