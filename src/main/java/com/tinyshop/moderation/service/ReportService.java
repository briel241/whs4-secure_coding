package com.tinyshop.moderation.service;

import com.tinyshop.chat.repository.ConversationRepository;
import com.tinyshop.chat.repository.MessageRepository;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.moderation.domain.Report;
import com.tinyshop.moderation.domain.ReportTargetType;
import com.tinyshop.moderation.dto.ReportForm;
import com.tinyshop.moderation.repository.ReportRepository;
import com.tinyshop.product.service.ProductService;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.user.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final ReportRepository reports;
    private final UserRepository userRepository;
    private final UserService users;
    private final ProductService products;
    private final MessageRepository messages;
    private final ConversationRepository conversations;

    public ReportService(ReportRepository reports, UserRepository userRepository, UserService users,
                         ProductService products, MessageRepository messages,
                         ConversationRepository conversations) {
        this.reports = reports;
        this.userRepository = userRepository;
        this.users = users;
        this.products = products;
        this.messages = messages;
        this.conversations = conversations;
    }

    @Transactional
    public Report create(UUID reporterId, ReportForm form) {
        validateTarget(reporterId, form.getTargetType(), form.getTargetId());
        if (reports.existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(
            reporterId, form.getTargetType(), form.getTargetId(),
            Instant.now().minus(24, ChronoUnit.HOURS))) {
            throw new BusinessException("같은 대상은 24시간에 한 번만 신고할 수 있습니다.");
        }
        return reports.save(new Report(users.require(reporterId), form.getTargetType(),
            form.getTargetId(), form.getReportType(), form.getDetails().strip()));
    }

    @Transactional(readOnly = true)
    public List<Report> mine(UUID reporterId) {
        return reports.findByReporterIdOrderByCreatedAtDesc(reporterId);
    }

    private void validateTarget(UUID reporterId, ReportTargetType type, UUID targetId) {
        if (type == ReportTargetType.USER) {
            if (reporterId.equals(targetId)) throw new BusinessException("자신은 신고할 수 없습니다.");
            if (!userRepository.existsById(targetId)) throw new NotFoundException("Target not found");
        } else if (type == ReportTargetType.PRODUCT) {
            products.detail(targetId, reporterId);
        } else if (type == ReportTargetType.MESSAGE) {
            var message = messages.findById(targetId).orElseThrow(() -> new NotFoundException("Target not found"));
            conversations.findParticipantConversation(message.getConversation().getId(), reporterId)
                .orElseThrow(() -> new NotFoundException("Target not found"));
            if (message.getSender().getId().equals(reporterId)) {
                throw new BusinessException("자신이 보낸 메시지는 신고할 수 없습니다.");
            }
        } else {
            throw new BusinessException("신고 대상을 선택해 주세요.");
        }
    }
}
