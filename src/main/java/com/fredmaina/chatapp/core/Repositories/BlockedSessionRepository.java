package com.fredmaina.chatapp.core.Repositories;

import com.fredmaina.chatapp.core.models.BlockedSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlockedSessionRepository extends JpaRepository<BlockedSession, UUID> {
    boolean existsByUserIdAndBlockedSessionId(UUID userId, String blockedSessionId);

    Optional<BlockedSession> findByUserIdAndBlockedSessionId(UUID userId, String blockedSessionId);
}
