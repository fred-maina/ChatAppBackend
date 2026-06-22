package com.fredmaina.chatapp.core.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportRequestDto {
    @NotBlank(message = "messageId is required")
    private String messageId;

    @NotBlank(message = "reason is required")
    @Size(max = 255, message = "reason must be at most 255 characters")
    private String reason;
}
