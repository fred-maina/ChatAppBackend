package com.fredmaina.chatapp.core.DTOs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class ChatMessageDto {
    private String id;
    private String text;
    private String senderType; // "anonymous" or "self"
    private String timestamp;
    private String nickname;
}
