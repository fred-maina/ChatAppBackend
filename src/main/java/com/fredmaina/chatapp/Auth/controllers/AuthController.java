package com.fredmaina.chatapp.Auth.controllers;


import com.fredmaina.chatapp.Auth.Dtos.*;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.services.AuthService;
import com.fredmaina.chatapp.Auth.services.JWTService;
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
        return ResponseEntity.status(401).body(authResponse);

    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody SignUpRequest signUpRequest) {
        AuthResponse authResponse = authService.signUp(signUpRequest);
        if(authResponse.isSuccess()){
            return ResponseEntity.ok(authResponse);
        }
        return ResponseEntity.status(500).body(authResponse);
    }
    @PostMapping("/oauth/google")
    public ResponseEntity<?> googleOAuth(@RequestBody GoogleOAuthRequest request) {
        AuthResponse response = authService.handleGoogleOAuth(request.getCode(), request.getRedirectUri());
        log.error(response.toString());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.getUsernameFromToken(token);
        User user = userRepository.findByEmail(email).orElseThrow();

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .success(true)
                        .token(token)
                        .user(user)
                        .build());
    }
    @PostMapping("/set-username")
    public ResponseEntity<AuthResponse> setUsername(@RequestBody Map<String,String> map) {
        String email = map.get("email");
        String username = map.get("username");
        AuthResponse authResponse = authService.setUsername(email,username);
        if(authResponse.isSuccess()){
            return ResponseEntity.ok(authResponse);
        }
        return ResponseEntity.status(404).body(authResponse);

    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Object>> checkUsername(@PathVariable String username) {
        try {
            boolean exists = userRepository.existsByUsername(username);
            if (exists) {

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "exists", true,
                        "username", username
                ));
            } else {
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