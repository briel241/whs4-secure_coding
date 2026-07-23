package com.tinyshop.moderation.domain;

import com.tinyshop.common.domain.BaseEntity;
import com.tinyshop.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "moderation_actions")
public class ModerationAction extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;
    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;
    @Column(name = "target_id", nullable = false)
    private UUID targetId;
    @Column(nullable = false, length = 50)
    private String action;
    @Column(name = "before_state", length = 100)
    private String beforeState;
    @Column(name = "after_state", length = 100)
    private String afterState;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(nullable = false, length = 30)
    private String result;

    protected ModerationAction() {
    }

    public ModerationAction(User admin, String targetType, UUID targetId, String action,
                            String beforeState, String afterState, String reason, String result) {
        this.admin = admin;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.reason = reason;
        this.result = result;
    }
}
