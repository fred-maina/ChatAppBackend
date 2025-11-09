package com.fredmaina.chatapp.core.Controllers;


import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.Auth.services.JWTService;
import com.fredmaina.chatapp.core.DTOs.ChatMessageDto;
import com.fredmaina.chatapp.core.DTOs.ChatSessionDto;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import com.fredmaina.chatapp.core.Services.ChatService;
import com.fredmaina.chatapp.core.error.ResourceNotFoundException;
import com.fredmaina.chatapp.core.error.UnauthorizedException;
import com.fredmaina.chatapp.core.models.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Not used in current methods, but good for future
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*; // Added DeleteMapping

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID; // For UUID

@Slf4j
@Controller
@RequestMapping("/api")
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private JWTService jwtService; // Corrected case from jWTService
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/chats")
    public ResponseEntity<?> getUserChats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw  new UnauthorizedException("Missing or invalid Authorization header");
        }

        String token = authHeader.replace("Bearer ", "");
        String email;
        try {
            email = jwtService.getUsernameFromToken(token);
        } catch (Exception e) {
            throw  new UnauthorizedException("Invalid token");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        User user = userOptional.get();
        List<ChatSessionDto> sessions = chatService.getUserChatSessions(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "chats", sessions));
    }

    @GetMapping("/chat/session_history")
    public ResponseEntity<?> getAnonChatHistory(
            @RequestParam String sessionId,
            @RequestParam String recipient // This is the username of the dashboard owner
    ) {
        ;
        List<ChatMessageDto> messages = chatService.getChatHistoryForAnonymous(sessionId, recipient);

        return ResponseEntity.ok(Map.of("success", true, "messages", messages));
    }

    @DeleteMapping("/chat/{anonSessionId}")
    public ResponseEntity<?> deleteChatSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String anonSessionId) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String token = authHeader.replace("Bearer ", "");
        String email;
        try {
            email = jwtService.getUsernameFromToken(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }
        User user = userOptional.get();

        try {
            chatService.deleteChatSession(user.getId(), anonSessionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Chat session deleted successfully."));
        } catch (RuntimeException e) {
            final String errMsg = String.format("Error deleting chat session %s",e.getMessage());
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }
    }
}