package com.fredmaina.chatapp.core.Services;

import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import com.fredmaina.chatapp.core.DTOs.ChatMessageDto;
import com.fredmaina.chatapp.core.DTOs.ChatMessageMapper;
import com.fredmaina.chatapp.core.DTOs.ChatSessionDto;
import com.fredmaina.chatapp.core.Repositories.ChatMessageRepository;
import com.fredmaina.chatapp.core.models.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {
    @Autowired
    ChatMessageRepository chatMessageRepository;
    @Autowired
    UserRepository userRepository;

    @Cacheable(value = "chatSessions", key = "#userId")
    @Transactional(readOnly = true)
    public List<ChatSessionDto> getUserChatSessions(UUID userId) {

        List<String> receivedFromSessionIds = chatMessageRepository.findDistinctSessionsByToUserId(userId);

        List<ChatSessionDto> sessions = new ArrayList<>();

        for (String sessionId : receivedFromSessionIds) {
            List<ChatMessage> allMessagesInSession = chatMessageRepository.findAllMessagesByUserAndSession(userId, sessionId);

            if (!allMessagesInSession.isEmpty()) {
                ChatMessage lastMsg = allMessagesInSession.getLast();

                String senderNickname = allMessagesInSession.stream()
                        .filter(m -> sessionId.equals(m.getFromSessionId()))
                        .map(ChatMessage::getNickname)
                        .findFirst()
                        .orElse("Anonymous");

                long unreadCount = chatMessageRepository.countUnreadMessagesForSession(userId, sessionId);

                ChatSessionDto sessionDto = ChatSessionDto.builder()
                        .id(sessionId)
                        .senderNickname(senderNickname)
                        .lastMessage(lastMsg.getContent())
                        .lastMessageTimestamp(lastMsg.getTimestamp()) // This will now be Instant
                        .unreadCount((int) unreadCount)
                        .avatarUrl("https://i.pravatar.cc/150?u=" + sessionId) // Placeholder
                        .messages(
                                allMessagesInSession.stream()
                                        .map(msg -> ChatMessageMapper.toDto(msg, userId))
                                        .collect(Collectors.toList())
                        )
                        .build();
                sessions.add(sessionDto);
            }
        }

        // Sort sessions by lastMessageTimestamp descending
        sessions.sort(Comparator.comparing(ChatSessionDto::getLastMessageTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        return sessions;
    }

    @Transactional
    public List<ChatMessageDto> getChatHistoryForAnonymous(String sessionId, String recipientUsername) {
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new RuntimeException("Recipient user not found: " + recipientUsername));

        List<ChatMessage> messages = chatMessageRepository.findFullChatHistory(sessionId,recipient.getId());

        chatMessageRepository.markMessagesAsRead(recipient.getId(), sessionId);

        return messages.stream()
                .map(msg -> ChatMessageMapper.toDto(msg, recipient.getId()))
                .collect(Collectors.toList());
    }


    @Transactional
    public void deleteChatSession(UUID userId, String anonSessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for deletion: " + userId));

        chatMessageRepository.deleteByFromSessionIdAndToUserId(anonSessionId, userId);
        chatMessageRepository.deleteByToSessionIdAndFromUserId(anonSessionId, userId);
    }
}