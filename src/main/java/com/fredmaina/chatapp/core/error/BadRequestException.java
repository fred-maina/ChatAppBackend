package com.fredmaina.chatapp.core.error;

public class BadRequestException extends RuntimeException{
    public BadRequestException(final String message) {
        super(message);
    }
}
