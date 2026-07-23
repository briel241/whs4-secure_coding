package com.tinyshop.wallet.domain;

import com.tinyshop.common.domain.BaseEntity;
import com.tinyshop.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_intents")
public class TransferIntent extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    @Column(nullable = false)
    private long amount;
    @Column(length = 200)
    private String memo;
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferIntentStatus status;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;

    protected TransferIntent() {
    }

    public TransferIntent(User sender, User recipient, long amount, String memo) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.memo = memo;
        this.idempotencyKey = UUID.randomUUID();
        this.status = TransferIntentStatus.PENDING;
        this.expiresAt = Instant.now().plusSeconds(180);
    }

    public void markUsed() {
        status = TransferIntentStatus.USED;
        usedAt = Instant.now();
    }

    public void markExpired() {
        status = TransferIntentStatus.EXPIRED;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public long getAmount() { return amount; }
    public String getMemo() { return memo; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public TransferIntentStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
}
