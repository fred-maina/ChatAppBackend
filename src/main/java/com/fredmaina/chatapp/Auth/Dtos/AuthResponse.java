package com.fredmaina.chatapp.Auth.Dtos;

import com.fredmaina.chatapp.Auth.Models.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@ToString
public class AuthResponse {
    private boolean success;
    private String message;
    private User user;
    private String token;


    public AuthResponse(boolean success, String message, User user, String token) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.token = token;
    }

}