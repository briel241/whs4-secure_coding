package com.tinyshop.moderation.domain;

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
@Table(name = "reports")
public class Report extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Column(nullable = false, length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "resolution_reason", length = 1000)
    private String resolutionReason;

    @ManyToOne
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Report() {
    }

    public Report(User reporter, ReportTargetType targetType, UUID targetId,
                  ReportType reportType, String details) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reportType = reportType;
        this.details = details;
        this.status = ReportStatus.OPEN;
    }

    public void resolve(ReportStatus status, String reason, User admin) {
        this.status = status;
        this.resolutionReason = reason;
        this.resolvedBy = admin;
        this.resolvedAt = status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED
            ? Instant.now() : null;
    }

    public User getReporter() { return reporter; }
    public ReportTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public ReportType getReportType() { return reportType; }
    public String getDetails() { return details; }
    public ReportStatus getStatus() { return status; }
    public String getResolutionReason() { return resolutionReason; }
    public User getResolvedBy() { return resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
}
