package com.fredmaina.chatapp.Auth.controllers;


import com.fredmaina.chatapp.Auth.Dtos.*;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.services.AuthService;
import com.fredmaina.chatapp.Auth.services.JWTService;
import com.fredmaina.chatapp.core.error.BadRequestException;
import com.fredmaina.chatapp.core.error.DataAlreadyExistsException;
import com.fredmaina.chatapp.core.error.ResourceNotFoundException;
import com.fredmaina.chatapp.core.error.UnauthorizedException;
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
        throw new UnauthorizedException("Invalid username or password");

    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody SignUpRequest signUpRequest) {
        AuthResponse authResponse = authService.signUp(signUpRequest);
        if(authResponse.isSuccess()){
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
        }
        if ("Username already exists (case-insensitive)".equals(authResponse.getMessage()) || "Email already exists".equals(authResponse.getMessage())) {
            throw new DataAlreadyExistsException("Username or email already exists");
        }
        throw new BadRequestException("Bad request"); // General bad request for other issues
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
            throw new UnauthorizedException("Error while contacting Google OAuth");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.getUsernameFromToken(token); // This actually gets the email (subject of token)
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            throw new ResourceNotFoundException("User not found for provided token.");
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
            throw new BadRequestException("Email and username are required.");
        }

        AuthResponse authResponse = authService.setUsername(email,username);
        if(authResponse.isSuccess()){
            return ResponseEntity.ok(authResponse);
        }
        // Distinguish between user not found and username taken
        if ("Username already taken (case-insensitive)".equals(authResponse.getMessage())) {
            throw new DataAlreadyExistsException(authResponse.getMessage());
        }
        throw new ResourceNotFoundException("User not found"); // Assuming "Invalid email" means user not found

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
                throw new ResourceNotFoundException("username not found");
            }
        } catch (Exception e) {
            log.error("Error checking username: {}", username, e);
            throw new RuntimeException("Error checking username");
        }
    }
}