package com.fredmaina.chatapp.Auth.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleMobileOAuthRequest {
    @NotBlank
    private String idToken;
}
