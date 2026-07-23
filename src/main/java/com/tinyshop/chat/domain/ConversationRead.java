package com.tinyshop.chat.domain;

import com.tinyshop.common.domain.BaseEntity;
import com.tinyshop.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "conversation_reads")
public class ConversationRead extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    protected ConversationRead() {
    }

    public ConversationRead(Conversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
        this.lastReadAt = Instant.now();
    }

    public void markRead() {
        this.lastReadAt = Instant.now();
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }
}
