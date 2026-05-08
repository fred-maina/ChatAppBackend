package com.fredmaina.chatapp.Auth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing one-time-use WebSocket connection tickets.
 * Tickets are stored in Redis with a short TTL (30 seconds) and are
 * immediately evicted upon validation to prevent reuse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketTicketService {

    private static final String TICKET_PREFIX = "ws-ticket:";
    private static final long TICKET_TTL_SECONDS = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Generates a one-time-use ticket for WebSocket connection.
     *
     * @param userEmail the email of the authenticated user
     * @return a unique ticket string
     */
    public String generateTicket(String userEmail) {
        String ticket = UUID.randomUUID().toString();
        String key = TICKET_PREFIX + ticket;
        
        redisTemplate.opsForValue().set(key, userEmail, TICKET_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Generated WebSocket ticket for user: {}", userEmail);
        
        return ticket;
    }

    /**
     * Validates and consumes a WebSocket ticket.
     * The ticket is immediately evicted upon successful validation to prevent reuse.
     *
     * @param ticket the ticket to validate
     * @return the user email if valid, null otherwise
     */
    public String validateAndConsumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }

        String key = TICKET_PREFIX + ticket;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            // Immediately evict the ticket to prevent reuse
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                String userEmail = value.toString();
                log.info("Validated and consumed WebSocket ticket for user: {}", userEmail);
                return userEmail;
            }
        }
        
        log.warn("Invalid or expired WebSocket ticket: {}", ticket);
        return null;
    }
}
