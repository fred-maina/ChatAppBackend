package com.fredmaina.chatapp.core.error;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    private static final String ERRMSG = "Not found";
    private static final int STATUS = 404;

    @Test
    void builder() {
        final ErrorResponse result = buildErrorResponse();
        Assertions.assertAll(()->assertEquals(STATUS, result.getStatus()),
                ()->assertEquals(ERRMSG, result.getMessage()),
                ()->assertEquals(ERRMSG, result.getError()));
    }

    final ErrorResponse buildErrorResponse(){
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(STATUS)
                .error(ERRMSG)
                .message(ERRMSG)
                .build();
    }
}