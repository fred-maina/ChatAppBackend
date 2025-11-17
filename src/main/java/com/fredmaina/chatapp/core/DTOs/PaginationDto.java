package com.fredmaina.chatapp.core.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginationDto {

        private List<ChatMessageDto> messages;
        private int page;
        private int size;
        private int totalPages;
        private boolean hasNextPage;
    }


