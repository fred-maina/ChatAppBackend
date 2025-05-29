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
    public ResponseEntity<UserDto> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.getUsernameFromToken(token);
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(new UserDto(user));
    }


}
