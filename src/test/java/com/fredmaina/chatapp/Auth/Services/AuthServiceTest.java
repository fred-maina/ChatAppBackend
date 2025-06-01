package com.fredmaina.chatapp.Auth.Services;

import com.fredmaina.chatapp.Auth.Dtos.AuthResponse;
import com.fredmaina.chatapp.Auth.Dtos.LoginRequest;
import com.fredmaina.chatapp.Auth.Dtos.SignUpRequest;
import com.fredmaina.chatapp.Auth.Models.Role;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.services.AuthService;
import com.fredmaina.chatapp.Auth.services.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSignUp_success() {
        SignUpRequest request = new SignUpRequest();
        request.setUsername("fredmaina123");
        request.setPassword("mypassword");
        request.setEmail("fred@example.com");
        request.setFirstName("Fredrick");
        request.setLastName("Maina");

        when(passwordEncoder.encode("mypassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.signUp(request);

        assertTrue(response.isSuccess());
        assertEquals("User registered and logged in successfully", response.getMessage());
        assertNotNull(response.getUser());
        assertEquals("fredmaina123", response.getUser().getUsername());
        assertEquals("encodedPassword", response.getUser().getPassword());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testSignUp_duplicateEmail() {
        SignUpRequest request = new SignUpRequest();
        request.setUsername("fredmaina123");
        request.setPassword("mypassword");
        request.setEmail("fred@example.com");

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(
                new DataIntegrityViolationException("users_email_key"));

        AuthResponse response = authService.signUp(request);

        assertFalse(response.isSuccess());
        assertEquals("Email already exists", response.getMessage());
    }

    @Test
    void testSignUp_duplicateUsername() {
        SignUpRequest request = new SignUpRequest();
        request.setUsername("fredmaina123");
        request.setPassword("mypassword");
        request.setEmail("fred@example.com");

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(
                new DataIntegrityViolationException("users_username_key"));

        AuthResponse response = authService.signUp(request);

        assertFalse(response.isSuccess());
        assertEquals("Username already exists", response.getMessage());
    }

    @Test
    void testLogin_success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("fredmaina123");
        loginRequest.setPassword("mypassword");

        User user = new User();
        user.setUsername("fredmaina123");
        user.setPassword("encodedPassword");
        user.setEmail("fred@example.com");

        when(userRepository.findByUsername("fredmaina123")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mypassword", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(user.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt-token", response.getToken());
        assertEquals(user, response.getUser());
    }

    @Test
    void testLogin_invalidCredentials() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("wronguser");
        loginRequest.setPassword("wrongpassword");

        when(userRepository.findByUsername("wronguser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("wronguser")).thenReturn(Optional.empty());

        AuthResponse response = authService.login(loginRequest);

        assertFalse(response.isSuccess());
        assertEquals("Invalid username/email or password", response.getMessage());
    }
}
