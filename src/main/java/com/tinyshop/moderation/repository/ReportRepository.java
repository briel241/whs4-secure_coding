package com.tinyshop.moderation.repository;

import com.tinyshop.moderation.domain.Report;
import com.tinyshop.moderation.domain.ReportStatus;
import com.tinyshop.moderation.domain.ReportTargetType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByReporterIdOrderByCreatedAtDesc(UUID reporterId);
    Page<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(
        UUID reporterId, ReportTargetType targetType, UUID targetId, Instant after);
    long countByStatus(ReportStatus status);
}
