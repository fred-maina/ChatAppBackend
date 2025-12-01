package com.fredmaina.chatapp.Auth.services;


import com.fredmaina.chatapp.Auth.Dtos.AuthResponse;
import com.fredmaina.chatapp.Auth.Dtos.GoogleOAuthRequest;
import com.fredmaina.chatapp.Auth.Dtos.LoginRequest;
import com.fredmaina.chatapp.Auth.Dtos.SignUpRequest;
import com.fredmaina.chatapp.Auth.Models.RefreshToken;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import org.springframework.http.*;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final RestTemplate restTemplate;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GoogleOAuthProperties googleOAuthProperties,
            RestTemplate restTemplate,
            JWTService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleOAuthProperties = googleOAuthProperties;
        this.restTemplate = restTemplate;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }


    // ---------------------------------------------------------------------------
    // SIGN UP
    // ---------------------------------------------------------------------------

    @CacheEvict(value = "usernameCheck", key = "#request.username.toLowerCase()")
    public AuthResponse signUp(SignUpRequest request) {
        AuthResponse authResponse = new AuthResponse();

        if (request.getUsername() != null &&
                userRepository.findByUsernameIgnoreCase(request.getUsername()).isPresent()) {
            authResponse.setMessage("Username already exists (case-insensitive)");
            authResponse.setSuccess(false);
            return authResponse;
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            authResponse.setMessage("Email already exists");
            authResponse.setSuccess(false);
            return authResponse;
        }

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

            // Generate tokens
            String accessToken = jwtService.generateToken(user.getEmail());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            authResponse.setMessage("User registered and logged in successfully");
            authResponse.setSuccess(true);
            authResponse.setUser(user);
            authResponse.setToken(accessToken);
            authResponse.setRefreshToken(refreshToken.getToken());

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation during sign up: {}", e.getMessage());

            if (e.getMessage().contains("users_email_key")) {
                authResponse.setMessage("Email already exists");
            } else if (e.getMessage().contains("idx_user_username")
                    || e.getMessage().contains("users_username_key")) {
                authResponse.setMessage("Username already exists");
            } else {
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


    // ---------------------------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------------------------

    public AuthResponse login(LoginRequest loginRequest) {
        AuthResponse authResponse = new AuthResponse();

        String input = loginRequest.getUsername();
        if (input == null || input.isBlank()) {
            authResponse.setSuccess(false);
            authResponse.setMessage("Username or email is required");
            return authResponse;
        }

        User user = userRepository.findByEmail(input)
                .or(() -> userRepository.findByUsernameIgnoreCase(input))
                .orElse(null);

        if (user == null ||
                user.getPassword() == null ||
                !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {

            authResponse.setSuccess(false);
            authResponse.setMessage("Invalid username/email or password");
            return authResponse;
        }

        String accessToken = jwtService.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        authResponse.setSuccess(true);
        authResponse.setMessage("Login successful");
        authResponse.setUser(user);
        authResponse.setToken(accessToken);
        authResponse.setRefreshToken(refreshToken.getToken());

        return authResponse;
    }


    // ---------------------------------------------------------------------------
    // GOOGLE OAUTH
    // ---------------------------------------------------------------------------

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

            HttpEntity<MultiValueMap<String, String>> tokenRequest =
                    new HttpEntity<>(params, headers);

            ResponseEntity<Map> tokenResponse =
                    restTemplate.postForEntity("https://oauth2.googleapis.com/token",
                            tokenRequest, Map.class);

            if (!tokenResponse.getStatusCode().is2xxSuccessful()
                    || tokenResponse.getBody() == null) {
                return new AuthResponse(false, "OAuth failed: Invalid token response", null, null,null);
            }

            String idToken = (String) tokenResponse.getBody().get("id_token");
            if (idToken == null) {
                return new AuthResponse(false, "OAuth failed: Missing id_token", null, null,null);
            }

            // Validate token
            String tokenInfoUrl =
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

            ResponseEntity<Map> tokenInfo =
                    restTemplate.getForEntity(tokenInfoUrl, Map.class);

            if (!tokenInfo.getStatusCode().is2xxSuccessful()
                    || tokenInfo.getBody() == null) {
                return new AuthResponse(false, "OAuth failed: Invalid token info", null, null,null);
            }

            Map body = tokenInfo.getBody();
            assert body != null;

            String email = (String) body.get("email");
            String firstName = (String) body.get("given_name");
            String lastName = (String) body.get("family_name");

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setVerified(true);
                newUser.setRole(Role.USER);
                return userRepository.save(newUser);
            });

            // Generate tokens
            String accessToken = jwtService.generateToken(user.getEmail());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return AuthResponse.builder()
                    .success(true)
                    .message("OAuth login successful")
                    .token(accessToken)
                    .refreshToken(refreshToken.getToken())
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


    // ---------------------------------------------------------------------------
    // USERNAME UPDATE
    // ---------------------------------------------------------------------------

    @CacheEvict(value = "usernameCheck", key = "#username.toLowerCase()")
    public AuthResponse setUsername(String email, String username) {

        Optional<User> userByUsername = userRepository.findByUsernameIgnoreCase(username);
        if (userByUsername.isPresent() &&
                !userByUsername.get().getEmail().equalsIgnoreCase(email)) {
            return AuthResponse.builder()
                    .message("Username already taken (case-insensitive)")
                    .success(false)
                    .build();
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            return AuthResponse.builder()
                    .message("Invalid email, user not found.")
                    .success(false)
                    .build();
        }

        User user = existing.get();
        user.setUsername(username);

        try {
            userRepository.save(user);
            return AuthResponse.builder()
                    .success(true)
                    .user(user)
                    .message("Username set successfully")
                    .build();

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while setting username: {}", e.getMessage());
            return AuthResponse.builder()
                    .message("Username already taken or another data issue occurred.")
                    .success(false)
                    .build();
        }
    }


    @Cacheable(value = "usernameCheck", key = "#username != null ? #username.toLowerCase() : 'null'")
    public Boolean checkUsernameExists(String username) {
        log.info("checking if username: {} exists", username);
        if (username == null) return false;
        return userRepository.existsByUsernameIgnoreCase(username);
    }
}
