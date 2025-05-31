package com.fredmaina.chatapp.core.Repositories;

import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.core.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    // Fetches messages TO a specific user (used as a base or for other features)
    List<ChatMessage> findByToUser(User toUser);

    // Fetches chat history for a specific anonymous session with a specific user (bidirectional)
    // Used by /api/chat/session_history

    @Query("SELECT m FROM ChatMessage m " +
            "WHERE " +
            "(m.fromSessionId = :sessionId AND m.toUser.id = :recipientId) " +
            "OR " +
            "(m.toSessionId = :sessionId AND m.fromUser.id = :recipientId) " +
            "ORDER BY m.timestamp")
    List<ChatMessage> findFullChatHistory(@Param("sessionId") String sessionId,
                                          @Param("recipientId") UUID recipientId);



    // Finds distinct anonymous session IDs that have messaged a specific user
    // Used by /api/chats to get all chat sessions for a user
    @Query("SELECT DISTINCT m.fromSessionId FROM ChatMessage m " +
            "WHERE m.toUser.id = :userId AND m.fromSessionId IS NOT NULL")
    List<String> findDistinctSessionsByToUserId(@Param("userId") UUID userId);

    // LEGACY/SPECIFIC: findByFromSessionIdAndToUserIdOrderByTimestampAsc - Retained if used elsewhere,
    // but getUserChatSessions will use a more comprehensive query.
    List<ChatMessage> findByFromSessionIdAndToUserIdOrderByTimestampAsc(String sessionId, UUID userId);

    // Fetches messages between a specific user and an anonymous session ID (bidirectional)
    // This is crucial for getting the correct last message and for building ChatSessionDto
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE (m.fromSessionId = :sessionId AND m.toUser.id = :userId) " +        // Anon to User
            "   OR (m.toSessionId = :sessionId AND m.fromUser.id = :userId) " +      // User to Anon
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findAllMessagesByUserAndSession(@Param("userId") UUID userId, @Param("sessionId") String sessionId);


    // Counts unread messages for a user from a specific anonymous session
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.toUser.id = :userId AND m.fromSessionId = :fromSessionId AND m.isRead = false")
    long countUnreadMessagesForSession(@Param("userId") UUID userId, @Param("fromSessionId") String fromSessionId);

    // Marks messages from an anonymous session to a user as read
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
            "WHERE m.toUser.id = :userId AND m.fromSessionId = :fromSessionId AND m.isRead = false")
    void markMessagesAsRead(@Param("userId") UUID userId, @Param("fromSessionId") String fromSessionId);

    // Deletes messages sent FROM an anonymous session TO a specific user.
    void deleteByFromSessionIdAndToUserId(String fromSessionId, UUID toUserId);

    // Deletes messages sent FROM a specific user TO an anonymous session.
    void deleteByToSessionIdAndFromUserId(String toSessionId, UUID fromUserId);

    // Used by /api/chat/session_history (Original version from prompt, ensure correct usage)
    List<ChatMessage> findByFromSessionIdAndToUser(String fromSessionId, User toUser);
}