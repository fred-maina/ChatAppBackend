package com.fredmaina.chatapp.core.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlockRequestDto {
    @NotBlank(message = "anonymousSessionId is required")
    @Size(max = 255, message = "anonymousSessionId must be at most 255 characters")
    private String anonymousSessionId;
}
