package com.fredmaina.chatapp.Auth.services;


import com.fredmaina.chatapp.Auth.Dtos.AuthResponse;
import com.fredmaina.chatapp.Auth.Dtos.GoogleOAuthRequest;
import com.fredmaina.chatapp.Auth.Dtos.LoginRequest;
import com.fredmaina.chatapp.Auth.Dtos.SignUpRequest;
import com.fredmaina.chatapp.Auth.Models.Role;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.configs.GoogleOAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JWTService jwtService;
    @Autowired
    GoogleOAuthProperties googleOAuthProperties;
    @Autowired
    RestTemplate restTemplate;

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

            String token = jwtService.generateToken(user.getEmail());

            authResponse.setMessage("User registered and logged in successfully");
            authResponse.setSuccess(true);
            authResponse.setUser(user);
            authResponse.setToken(token);
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

        String input = loginRequest.getUsername();
        if (input == null || input.isBlank()) {
            authResponse.setSuccess(false);
            authResponse.setMessage("Username or email is required");
            return authResponse;
        }

        User user = null;

        // Try email lookup first (most reliable, especially with Google OAuth users)
        Optional<User> byEmail = userRepository.findByEmail(input);
        if (byEmail.isPresent()) {
            user = byEmail.get();
        } else {
            // Then try username lookup, but skip if the input equals "null"
            Optional<User> byUsername = userRepository.findByUsername(input);
            if (byUsername.isPresent()) {
                user = byUsername.get();
            }
        }

        if (user == null || user.getPassword() == null ||
                !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
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



    public AuthResponse handleGoogleOAuth(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", googleOAuthProperties.getClientId());
            params.add("client_secret", googleOAuthProperties.getClientSecret());
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", tokenRequest, Map.class
            );

            assert tokenResponse.getBody() != null;
            String idToken = (String) tokenResponse.getBody().get("id_token");

            // Step 2: Validate id_token using Google's tokeninfo endpoint
            ResponseEntity<Map> tokenInfo = restTemplate.getForEntity(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken, Map.class
            );

            Map body = tokenInfo.getBody();

            assert body != null;
            String email = (String) body.get("email");
            String firstName = (String) body.get("given_name");
            String lastName = (String) body.get("family_name");

            // Step 3: Register or retrieve user
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user = existingUser.orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setVerified(true);
                newUser.setRole(Role.USER);
                return userRepository.save(newUser);
            });

            String token = jwtService.generateToken(user.getEmail());

            return AuthResponse.builder()
                    .success(true)
                    .message("OAuth login successful")
                    .token(token)
                    .user(user)
                    .build();

        } catch (Exception e) {
            return AuthResponse.builder()
                    .success(false)
                    .message("OAuth failed: " + e.getMessage())
                    .build();
        }
    }

}
