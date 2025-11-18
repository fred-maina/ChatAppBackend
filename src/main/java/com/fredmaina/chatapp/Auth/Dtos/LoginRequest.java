package com.fredmaina.chatapp.Auth.Dtos;

import lombok.Data;

import java.time.Instant;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private final Instant lastLoginAt = Instant.now();
}
