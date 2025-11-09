package com.fredmaina.chatapp.core.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class ChatAppExceptionHandlerTest {
    private final ChatAppExceptionHandler exceptionHandler = new ChatAppExceptionHandler();

    @Test
    void badRequestHandler() {
        // Given
        String errorMessage = "Invalid request!";
        BadRequestException ex = new BadRequestException(errorMessage);
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.badRequestHandler(ex);
        // Then
        assertResult(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void unauthorizedHandler() {
        // Given
        String errorMessage = "Your are not authorized!";
        UnauthorizedException ex = new UnauthorizedException(errorMessage);
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.unauthorizedHandler(ex);
        // Then
        assertResult(response, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resourceNotFoundHandler() {
        // Given
        String errorMessage = "User not found";
        ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.resourceNotFoundHandler(ex);
        // Then
        assertResult(response, HttpStatus.NOT_FOUND);
    }

    @Test
    void duplicateDataRequestHandler() {
        // Given
        String errorMessage = "User already exists";
        DataAlreadyExistsException ex = new DataAlreadyExistsException(errorMessage);
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.duplicateDataRequestHandler(ex);
        // Then
        assertResult(response, HttpStatus.CONFLICT);
    }

    @Test
    void handleGlobalException() {
        // Given
        String errorMessage = "User already exists";
        Exception ex = new RuntimeException(errorMessage);
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(ex);
        // Then
        assertResult(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void assertResult(ResponseEntity<ErrorResponse> response, HttpStatus status){
        assertNotNull(response);
        assertEquals(status, response.getStatusCode());
    }
}