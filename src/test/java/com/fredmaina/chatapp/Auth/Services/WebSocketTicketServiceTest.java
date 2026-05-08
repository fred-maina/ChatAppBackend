package com.fredmaina.chatapp.Auth.Services;

import com.fredmaina.chatapp.Auth.services.WebSocketTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebSocketTicketServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private WebSocketTicketService webSocketTicketService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGenerateTicket_success() {
        String userEmail = "test@example.com";

        String ticket = webSocketTicketService.generateTicket(userEmail);

        assertNotNull(ticket);
        assertFalse(ticket.isBlank());
        // Verify that the ticket was stored in Redis with 30 second TTL
        verify(valueOperations).set(
            eq("ws-ticket:" + ticket),
            eq(userEmail),
            eq(30L),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testGenerateTicket_uniqueTickets() {
        String userEmail = "test@example.com";

        String ticket1 = webSocketTicketService.generateTicket(userEmail);
        String ticket2 = webSocketTicketService.generateTicket(userEmail);

        assertNotNull(ticket1);
        assertNotNull(ticket2);
        assertNotEquals(ticket1, ticket2);
    }

    @Test
    void testValidateAndConsumeTicket_validTicket() {
        String ticket = "valid-ticket-uuid";
        String userEmail = "test@example.com";
        String key = "ws-ticket:" + ticket;

        when(valueOperations.get(key)).thenReturn(userEmail);
        when(redisTemplate.delete(key)).thenReturn(true);

        String result = webSocketTicketService.validateAndConsumeTicket(ticket);

        assertEquals(userEmail, result);
        // Verify the ticket was deleted after validation
        verify(redisTemplate).delete(key);
    }

    @Test
    void testValidateAndConsumeTicket_invalidTicket() {
        String ticket = "invalid-ticket-uuid";
        String key = "ws-ticket:" + ticket;

        when(valueOperations.get(key)).thenReturn(null);

        String result = webSocketTicketService.validateAndConsumeTicket(ticket);

        assertNull(result);
        // Verify delete was not called since ticket doesn't exist
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testValidateAndConsumeTicket_nullTicket() {
        String result = webSocketTicketService.validateAndConsumeTicket(null);

        assertNull(result);
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void testValidateAndConsumeTicket_blankTicket() {
        String result = webSocketTicketService.validateAndConsumeTicket("   ");

        assertNull(result);
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void testValidateAndConsumeTicket_deleteFailure() {
        String ticket = "ticket-uuid";
        String userEmail = "test@example.com";
        String key = "ws-ticket:" + ticket;

        when(valueOperations.get(key)).thenReturn(userEmail);
        when(redisTemplate.delete(key)).thenReturn(false);

        String result = webSocketTicketService.validateAndConsumeTicket(ticket);

        // If delete fails, should return null (ticket might have been consumed already)
        assertNull(result);
    }
}
