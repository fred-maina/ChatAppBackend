package com.fredmaina.chatapp.Auth.services;


import com.fredmaina.chatapp.Auth.Dtos.AuthResponse;
import com.fredmaina.chatapp.Auth.Dtos.GoogleOAuthRequest;
import com.fredmaina.chatapp.Auth.Dtos.LoginRequest;
import com.fredmaina.chatapp.Auth.Dtos.SignUpRequest;
import com.fredmaina.chatapp.Auth.Models.Role;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.configs.GoogleOAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
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


    @CacheEvict(value = "usernameCheck", key = "#user.username.toLowerCase()")
    public AuthResponse signUp(SignUpRequest request) {
        AuthResponse authResponse = new AuthResponse();

        // Case-insensitive check for username
        if (request.getUsername() != null && userRepository.findByUsernameIgnoreCase(request.getUsername()).isPresent()) {
            authResponse.setMessage("Username already exists (case-insensitive)");
            authResponse.setSuccess(false);
            return authResponse;
        }

        // Check for existing email (which is already case-sensitive by default in most DBs or handled by your existing logic)
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            authResponse.setMessage("Email already exists");
            authResponse.setSuccess(false);
            return authResponse;
        }


        User user = new User();
        user.setUsername(request.getUsername()); // Store the original case
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
            // This catch block might still be useful for other constraints,
            // but the primary username and email checks are now done above.
            if (e.getMessage().contains("users_email_key")) { // Should ideally be caught by the check above
                authResponse.setMessage("Email already exists");
            } else if (e.getMessage().contains("idx_user_username") || e.getMessage().contains("users_username_key")) { // Should ideally be caught by the check above
                authResponse.setMessage("Username already exists");
            } else {
                log.error("Data integrity violation during sign up: {}", e.getMessage());
                authResponse.setMessage("Data integrity violation");
            }
            authResponse.setSuccess(false);
        } catch (Exception e) {
            log.error("Unexpected error during sign up: {}", e.getMessage(), e);
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

        // Try email lookup first
        Optional<User> byEmail = userRepository.findByEmail(input);
        if (byEmail.isPresent()) {
            user = byEmail.get();
        } else {
            // Then try username lookup (case-insensitive for login flexibility, if desired)
            // If you want case-sensitive login for username, use userRepository.findByUsername(input)
            Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(input);
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


    @CacheEvict(value = "usernameCheck", key = "#user.username.toLowerCase()")
    public AuthResponse handleGoogleOAuth(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Prepare form parameters
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", googleOAuthProperties.getClientId());
            params.add("client_secret", googleOAuthProperties.getClientSecret());
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            log.info("Sending token request with params: {}", params);

            // Build the request
            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

            // Step 1: Request access token
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", tokenRequest, Map.class);

            log.info("Token response status: {}", tokenResponse.getStatusCode());
            log.debug("Token response body: {}", tokenResponse.getBody());

            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                log.error("Failed to retrieve token: HTTP {} - Body: {}", tokenResponse.getStatusCode(), tokenResponse.getBody());
                return new AuthResponse(false, "OAuth failed: Invalid token response", null, null);
            }

            String idToken = (String) tokenResponse.getBody().get("id_token");
            if (idToken == null) {
                log.error("No id_token found in token response");
                return new AuthResponse(false, "OAuth failed: Missing id_token", null, null);
            }

            // Step 2: Validate id_token
            String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            log.info("Validating id_token via: {}", tokenInfoUrl);

            ResponseEntity<Map> tokenInfo = restTemplate.getForEntity(tokenInfoUrl, Map.class);

            log.info("Token info response status: {}", tokenInfo.getStatusCode());
            log.debug("Token info response body: {}", tokenInfo.getBody());

            if (tokenInfo.getStatusCode() != HttpStatus.OK || tokenInfo.getBody() == null) {
                log.error("Token info validation failed: HTTP {} - Body: {}", tokenInfo.getStatusCode(), tokenInfo.getBody());
                return new AuthResponse(false, "OAuth failed: Invalid token info", null, null);
            }

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
                // For Google OAuth users, username can be initially null or set to a default
                // or prompt them to set it later.
                // newUser.setUsername(null); // Or some generated unique value if needed immediately
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
            log.error("OAuth failed: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("OAuth failed: " + e.getMessage())
                    .build();
        }
    }

    @CacheEvict(value = "usernameCheck", key = "#user.username.toLowerCase()")
    public AuthResponse setUsername(String email, String username) {
        // Check if the desired username (case-insensitive) is already taken by another user
        Optional<User> userByUsername = userRepository.findByUsernameIgnoreCase(username);
        if (userByUsername.isPresent() && !userByUsername.get().getEmail().equalsIgnoreCase(email)) {
            return AuthResponse.builder()
                    .message("Username already taken (case-insensitive)")
                    .success(false)
                    .build();
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            User user = existingUserByEmail.get();
            user.setUsername(username); // Store with original casing
            try {
                userRepository.save(user);
                return AuthResponse.builder()
                        .success(true)
                        .user(user)
                        .message("Username set successfully")
                        .build();
            } catch (DataIntegrityViolationException e) {

                log.error("Data integrity violation while setting username for email {}: {}", email, e.getMessage());
                return AuthResponse.builder()
                        .message("Username already taken or another data issue occurred.")
                        .success(false)
                        .build();
            }
        }
        return AuthResponse.builder()
                .message("Invalid email, user not found.")
                .success(false)
                .build();
    }
    @Cacheable(value = "usernameCheck", key = "#username != null ? #username.toLowerCase() : 'null'")
    public boolean checkUsernameExists(String username) {
        log.info("checking if username: {} exists",username);
        if (username == null) return false;
        return userRepository.existsByUsernameIgnoreCase(username);
    }


}