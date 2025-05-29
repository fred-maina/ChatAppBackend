package com.fredmaina.chatapp.Auth.Dtos;

import lombok.Data;

@Data
public class GoogleOAuthRequest {
    private String code;
    private String redirectUri;
}
