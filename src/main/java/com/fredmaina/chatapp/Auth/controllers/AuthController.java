package com.fredmaina.chatapp.Auth.controllers;


import com.fredmaina.chatapp.Auth.Dtos.*;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.services.AuthService;
import com.fredmaina.chatapp.Auth.services.JWTService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Slf4j
@Controller
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthService authService;
    @Autowired
    JWTService jwtService;
    @Autowired
    UserRepository userRepository;


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        if(authResponse.isSuccess()){
            return ResponseEntity.ok(authResponse);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authResponse);

    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody SignUpRequest signUpRequest) {
        AuthResponse authResponse = authService.signUp(signUpRequest);
        if(authResponse.isSuccess()){
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
        }
        if ("Username already exists (case-insensitive)".equals(authResponse.getMessage()) || "Email already exists".equals(authResponse.getMessage())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(authResponse);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(authResponse); // General bad request for other issues
    }
    @PostMapping("/oauth/google")
    public ResponseEntity<?> googleOAuth(@RequestBody GoogleOAuthRequest request) {
        AuthResponse response = authService.handleGoogleOAuth(request.getCode(), request.getRedirectUri());
        // log.error(response.toString()); // log.info or log.debug might be more appropriate for successful responses
        if (response.isSuccess()) {
            log.info("Google OAuth successful for user: {}", response.getUser() != null ? response.getUser().getEmail() : "Unknown");
            return ResponseEntity.ok(response);
        } else {
            log.warn("Google OAuth failed: {}", response.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.getUsernameFromToken(token); // This actually gets the email (subject of token)
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.builder().success(false).message("User not found for provided token.").build());
        }

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .success(true)
                        .token(token) // Consider if sending the token back is necessary here
                        .user(user)
                        .build());
    }
    @PostMapping("/set-username")
    public ResponseEntity<AuthResponse> setUsername(@RequestBody Map<String,String> map) {
        String email = map.get("email");
        String username = map.get("username");

        if (email == null || email.isBlank() || username == null || username.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder().success(false).message("Email and username are required.").build());
        }

        AuthResponse authResponse = authService.setUsername(email,username);
        if(authResponse.isSuccess()){
            return ResponseEntity.ok(authResponse);
        }
        // Distinguish between user not found and username taken
        if ("Username already taken (case-insensitive)".equals(authResponse.getMessage())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(authResponse);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(authResponse); // Assuming "Invalid email" means user not found

    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Object>> checkUsername(@PathVariable String username) {
        try {
            boolean exists = authService.checkUsernameExists(username);
            if (exists) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "exists", true,
                        "username", username
                ));
            } else {
                log.error("username {} not found for some weird reason", username);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "exists", false
                ));
            }
        } catch (Exception e) {
            log.error("Error checking username: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error checking username"
            ));
        }
    }
}