package com.fredmaina.chatapp.core.Repositories;

import com.fredmaina.chatapp.core.models.ReportedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportedMessageRepository extends JpaRepository<ReportedMessage, UUID> {
}
