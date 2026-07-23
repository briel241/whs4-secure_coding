package com.tinyshop.chat.repository;

import com.tinyshop.chat.domain.ConversationRead;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationReadRepository extends JpaRepository<ConversationRead, UUID> {
    Optional<ConversationRead> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}
