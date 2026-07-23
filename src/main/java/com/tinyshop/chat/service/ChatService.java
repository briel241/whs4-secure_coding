package com.tinyshop.chat.service;

import com.tinyshop.chat.domain.Conversation;
import com.tinyshop.chat.domain.ConversationRead;
import com.tinyshop.chat.domain.Message;
import com.tinyshop.chat.repository.ConversationReadRepository;
import com.tinyshop.chat.repository.ConversationRepository;
import com.tinyshop.chat.repository.MessageRepository;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.product.service.ProductService;
import com.tinyshop.user.domain.UserStatus;
import com.tinyshop.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ConversationReadRepository reads;
    private final ProductService products;
    private final UserService users;
    private final MessageRateLimiter rateLimiter;

    public ChatService(ConversationRepository conversations, MessageRepository messages,
                       ConversationReadRepository reads, ProductService products,
                       UserService users, MessageRateLimiter rateLimiter) {
        this.conversations = conversations;
        this.messages = messages;
        this.reads = reads;
        this.products = products;
        this.users = users;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public Conversation open(UUID productId, UUID buyerId) {
        var product = products.detail(productId, buyerId);
        if (product.getSeller().getId().equals(buyerId)) {
            throw new BusinessException("자신의 상품에는 문의할 수 없습니다.");
        }
        return conversations.findByProductIdAndBuyerIdAndSellerId(
                productId, buyerId, product.getSeller().getId())
            .orElseGet(() -> {
                try {
                    return conversations.saveAndFlush(new Conversation(product,
                        users.require(buyerId), product.getSeller()));
                } catch (DataIntegrityViolationException exception) {
                    return conversations.findByProductIdAndBuyerIdAndSellerId(
                            productId, buyerId, product.getSeller().getId())
                        .orElseThrow(() -> exception);
                }
            });
    }

    @Transactional(readOnly = true)
    public List<Conversation> list(UUID userId) {
        return conversations.findAllForParticipant(userId);
    }

    @Transactional
    public Conversation view(UUID conversationId, UUID userId) {
        Conversation conversation = requireParticipant(conversationId, userId);
        reads.findByConversationIdAndUserId(conversationId, userId)
            .ifPresentOrElse(ConversationRead::markRead,
                () -> reads.save(new ConversationRead(conversation, users.require(userId))));
        return conversation;
    }

    @Transactional(readOnly = true)
    public List<Message> visibleMessages(UUID conversationId, UUID userId) {
        requireParticipant(conversationId, userId);
        return messages.findByConversationIdAndHiddenFalseOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public Message send(UUID conversationId, UUID senderId, String rawBody) {
        rateLimiter.check(senderId);
        Conversation conversation = requireParticipant(conversationId, senderId);
        var sender = users.require(senderId);
        if (sender.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("현재 계정 상태에서는 메시지를 보낼 수 없습니다.");
        }
        String body = rawBody == null ? "" : rawBody.strip();
        if (body.isBlank() || body.length() > 1000) {
            throw new BusinessException("메시지는 1~1,000자의 일반 텍스트여야 합니다.");
        }
        Message message = messages.save(new Message(conversation, sender, body));
        conversation.touch();
        return message;
    }

    @Transactional(readOnly = true)
    public long unread(UUID conversationId, UUID userId) {
        requireParticipant(conversationId, userId);
        Instant since = reads.findByConversationIdAndUserId(conversationId, userId)
            .map(ConversationRead::getLastReadAt).orElse(Instant.EPOCH);
        return messages.countUnread(conversationId, userId, since);
    }

    private Conversation requireParticipant(UUID id, UUID userId) {
        return conversations.findParticipantConversation(id, userId)
            .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }
}
