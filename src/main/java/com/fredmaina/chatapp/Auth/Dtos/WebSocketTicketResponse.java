package com.fredmaina.chatapp.Auth.Dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketTicketResponse {
    private boolean success;
    private String message;
    private String ticket;
}
