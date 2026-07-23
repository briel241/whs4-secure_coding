package com.tinyshop.wallet.domain;

import com.tinyshop.common.domain.BaseEntity;
import com.tinyshop.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer extends BaseEntity {
    @OneToOne(optional = false)
    @JoinColumn(name = "intent_id", nullable = false, unique = true)
    private TransferIntent intent;
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
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;
    @ManyToOne
    @JoinColumn(name = "reference_transfer_id")
    private Transfer referenceTransfer;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected Transfer() {
    }

    public Transfer(TransferIntent intent) {
        this.intent = intent;
        this.sender = intent.getSender();
        this.recipient = intent.getRecipient();
        this.amount = intent.getAmount();
        this.memo = intent.getMemo();
        this.idempotencyKey = intent.getIdempotencyKey();
        this.status = TransferStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public TransferIntent getIntent() { return intent; }
    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public long getAmount() { return amount; }
    public String getMemo() { return memo; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public TransferStatus getStatus() { return status; }
    public Transfer getReferenceTransfer() { return referenceTransfer; }
    public Instant getCompletedAt() { return completedAt; }
}
