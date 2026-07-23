package com.tinyshop.chat.repository;

import com.tinyshop.chat.domain.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByProductIdAndBuyerIdAndSellerId(UUID productId, UUID buyerId, UUID sellerId);

    @Query("""
        select c from Conversation c
        join fetch c.product p join fetch c.buyer join fetch c.seller
        where c.id = :id and (c.buyer.id = :userId or c.seller.id = :userId)
        """)
    Optional<Conversation> findParticipantConversation(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
        select c from Conversation c
        join fetch c.product p join fetch c.buyer join fetch c.seller
        where c.buyer.id = :userId or c.seller.id = :userId
        order by c.updatedAt desc
        """)
    List<Conversation> findAllForParticipant(@Param("userId") UUID userId);
}
