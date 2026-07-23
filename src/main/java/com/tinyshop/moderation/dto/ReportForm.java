package com.tinyshop.moderation.dto;

import com.tinyshop.moderation.domain.ReportTargetType;
import com.tinyshop.moderation.domain.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class ReportForm {
    @NotNull(message = "신고 대상을 선택해 주세요.")
    private ReportTargetType targetType;
    @NotNull(message = "신고 대상을 선택해 주세요.")
    private UUID targetId;
    @NotNull(message = "신고 유형을 선택해 주세요.")
    private ReportType reportType;
    @NotBlank(message = "신고 내용을 입력해 주세요.")
    @Size(min = 10, max = 1000, message = "신고 내용은 10~1,000자여야 합니다.")
    private String details;

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
