package com.fredmaina.chatapp.core.DTOs;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WebSocketMessagePayload {
    private MessageType type; // e.g., "ANON_TO_USER" or "USER_TO_ANON"
    private String from; // username or sessionId
    private String to;   // username or sessionId
    private String content;
    private String nickname;
    private String timestamp;
    private String ChatId;


    public WebSocketMessagePayload(MessageType type, String from, String to, String content, String nickname, String timestamp) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;
        this.nickname = nickname;
        this.timestamp = timestamp;
    }
}
