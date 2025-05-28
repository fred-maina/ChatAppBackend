package com.fredmaina.chatapp.Auth.services;


import com.fredmaina.chatapp.Auth.Dtos.AuthResponse;
import com.fredmaina.chatapp.Auth.Dtos.LoginRequest;
import com.fredmaina.chatapp.Auth.Dtos.SignUpRequest;
import com.fredmaina.chatapp.Auth.Models.Role;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JWTService jwtService;

    public AuthResponse signUp(SignUpRequest request) {
        AuthResponse authResponse = new AuthResponse();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setVerified(true);
        user.setRole(Role.USER);
        try {
            userRepository.save(user);
            authResponse.setMessage("User registered successfully");
            authResponse.setSuccess(true);
            authResponse.setUser(user);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("users_email_key")) {
                authResponse.setMessage("Email already exists");
            } else if (e.getMessage().contains("idx_user_username") || e.getMessage().contains("users_username_key")) {
                authResponse.setMessage("Username already exists");
            } else {
                authResponse.setMessage("Data integrity violation");
            }
            authResponse.setSuccess(false);
        } catch (Exception e) {
            authResponse.setMessage("Unexpected error occurred");
            authResponse.setSuccess(false);
        }
        return authResponse;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        AuthResponse authResponse = new AuthResponse();

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(userRepository.findByEmail(loginRequest.getUsername()).orElse(null));

        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            authResponse.setSuccess(false);
            authResponse.setMessage("Invalid username/email or password");
            return authResponse;
        }

        String token = jwtService.generateToken(user.getEmail());
        authResponse.setSuccess(true);
        authResponse.setMessage("Login successful");
        authResponse.setUser(user);
        authResponse.setToken(token);
        return authResponse;
    }

}
