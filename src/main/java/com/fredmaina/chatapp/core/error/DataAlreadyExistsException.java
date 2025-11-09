package com.fredmaina.chatapp.core.error;

public class DataAlreadyExistsException extends RuntimeException{
    public DataAlreadyExistsException(final String message) {
        super(message);
    }
}
