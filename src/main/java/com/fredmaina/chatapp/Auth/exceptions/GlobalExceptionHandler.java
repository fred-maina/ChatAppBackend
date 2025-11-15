package com.fredmaina.chatapp.Auth.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {

            Map<String, String> errors = ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .collect(Collectors.toMap(
                            err -> err.getField(),
                            err -> err.getDefaultMessage(),
                            (a, b) -> a
                    ));

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "timestamp", LocalDateTime.now().toString(),
                            "message", "Validation failed",
                            "errors", errors
                    )
            );
        }
    }

