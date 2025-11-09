package com.fredmaina.chatapp.core.error;

public class UnauthorizedException extends RuntimeException{
    public UnauthorizedException(final String message) {
        super(message);
    }
}
