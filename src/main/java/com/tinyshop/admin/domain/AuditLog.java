package com.tinyshop.admin.domain;

import com.tinyshop.common.domain.BaseEntity;
import com.tinyshop.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;
    @Column(nullable = false, length = 100)
    private String action;
    @Column(name = "target_type", length = 50)
    private String targetType;
    @Column(name = "target_id")
    private UUID targetId;
    @Column(nullable = false, length = 30)
    private String result;
    @Column(length = 1000)
    private String reason;
    @Column(length = 2000)
    private String metadata;

    protected AuditLog() {
    }

    public AuditLog(User actor, String action, String targetType, UUID targetId,
                    String result, String reason, String metadata) {
        this.actor = actor;
        this.action = sanitize(action);
        this.targetType = sanitize(targetType);
        this.targetId = targetId;
        this.result = sanitize(result);
        this.reason = sanitize(reason);
        this.metadata = sanitize(metadata);
    }

    private static String sanitize(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n\\t]", "_");
    }

    public User getActor() { return actor; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public String getResult() { return result; }
    public String getReason() { return reason; }
    public String getMetadata() { return metadata; }
}
