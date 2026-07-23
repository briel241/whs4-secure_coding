package com.tinyshop.chat.repository;

import com.tinyshop.chat.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdAndHiddenFalseOrderByCreatedAtAsc(UUID conversationId);
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("""
        select count(m) from Message m
        where m.conversation.id = :conversationId and m.hidden = false
          and m.sender.id <> :userId and m.createdAt > :since
        """)
    long countUnread(@Param("conversationId") UUID conversationId,
                     @Param("userId") UUID userId, @Param("since") Instant since);
}
