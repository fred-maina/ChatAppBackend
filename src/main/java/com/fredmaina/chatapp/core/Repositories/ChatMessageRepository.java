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

    List<ChatMessage> findByToUser(User toUser);

    @Query("SELECT m FROM ChatMessage m " +
            "WHERE " +
            "(m.fromSessionId = :sessionId AND m.toUser.id = :recipientId) " +
            "OR " +
            "(m.toSessionId = :sessionId AND m.fromUser.id = :recipientId) " +
            "ORDER BY m.timestamp")
    List<ChatMessage> findFullChatHistory(@Param("sessionId") String sessionId,
                                          @Param("recipientId") UUID recipientId);

    @Query("SELECT DISTINCT m.fromSessionId FROM ChatMessage m " +
            "WHERE m.toUser.id = :userId AND m.fromSessionId IS NOT NULL")
    List<String> findDistinctSessionsByToUserId(@Param("userId") UUID userId);




    @Query("SELECT m FROM ChatMessage m " +
            "WHERE (m.fromSessionId = :sessionId AND m.toUser.id = :userId) " +
            "   OR (m.toSessionId = :sessionId AND m.fromUser.id = :userId) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findAllMessagesByUserAndSession(@Param("userId") UUID userId, @Param("sessionId") String sessionId);

    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.toUser.id = :userId AND m.fromSessionId = :fromSessionId AND m.isRead = false")
    long countUnreadMessagesForSession(@Param("userId") UUID userId, @Param("fromSessionId") String fromSessionId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
            "WHERE m.toUser.id = :userId AND m.fromSessionId = :fromSessionId AND m.isRead = false")
    void markMessagesAsRead(@Param("userId") UUID userId, @Param("fromSessionId") String fromSessionId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead =true " +
    "WHERE m.fromSessionId=:sessionId AND m.isRead=false")
    void markMessagesAsRead(String sessionId);

    void deleteByFromSessionIdAndToUserId(String fromSessionId, UUID toUserId);

    void deleteByToSessionIdAndFromUserId(String toSessionId, UUID fromUserId);

}