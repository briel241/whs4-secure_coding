package com.tinyshop.admin.service;

import com.tinyshop.admin.domain.AuditLog;
import com.tinyshop.admin.repository.AuditLogRepository;
import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.chat.domain.Message;
import com.tinyshop.chat.repository.MessageRepository;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.moderation.domain.ModerationAction;
import com.tinyshop.moderation.domain.Report;
import com.tinyshop.moderation.domain.ReportStatus;
import com.tinyshop.moderation.repository.ModerationActionRepository;
import com.tinyshop.moderation.repository.ReportRepository;
import com.tinyshop.product.domain.Category;
import com.tinyshop.product.domain.ModerationStatus;
import com.tinyshop.product.domain.Product;
import com.tinyshop.product.repository.CategoryRepository;
import com.tinyshop.product.repository.ProductRepository;
import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.user.domain.UserStatus;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.user.service.UserService;
import com.tinyshop.wallet.domain.WalletStatus;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final UserService userService;
    private final ProductRepository products;
    private final MessageRepository messages;
    private final ReportRepository reports;
    private final CategoryRepository categories;
    private final WalletAccountRepository wallets;
    private final ModerationActionRepository actions;
    private final AuditLogRepository audits;

    public AdminService(UserRepository users, UserService userService, ProductRepository products,
                        MessageRepository messages, ReportRepository reports,
                        CategoryRepository categories, WalletAccountRepository wallets,
                        ModerationActionRepository actions, AuditLogRepository audits) {
        this.users = users;
        this.userService = userService;
        this.products = products;
        this.messages = messages;
        this.reports = reports;
        this.categories = categories;
        this.wallets = wallets;
        this.actions = actions;
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public DashboardCounts counts() {
        return new DashboardCounts(users.count(), products.count(), reports.countByStatus(ReportStatus.OPEN),
            audits.count());
    }

    @Transactional(readOnly = true)
    public Page<User> users(String keyword, int page) {
        var pageable = PageRequest.of(Math.max(0, page), 30);
        return keyword == null || keyword.isBlank() ? users.findAll(pageable)
            : users.findByNicknameContainingIgnoreCase(keyword.strip(), pageable);
    }

    @Transactional(readOnly = true)
    public User user(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public void changeUserStatus(UUID adminId, UUID targetId, UserStatus status, String reason) {
        String safeReason = requireReason(reason);
        User admin = user(adminId);
        User target = user(targetId);
        if (target.getRole() == UserRole.ADMIN && target.getStatus() == UserStatus.ACTIVE
            && status != UserStatus.ACTIVE
            && users.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new BusinessException("마지막 활성 관리자 계정은 중지하거나 차단할 수 없습니다.");
        }
        UserStatus before = target.getStatus();
        target.changeStatus(status);
        wallets.findByUserId(targetId).ifPresent(wallet -> wallet.changeStatus(
            status == UserStatus.ACTIVE ? WalletStatus.ACTIVE : WalletStatus.FROZEN));
        record(admin, "USER", targetId, "CHANGE_STATUS", before.name(), status.name(), safeReason);
        if (status != UserStatus.ACTIVE) userService.expireSessions(targetId);
    }

    @Transactional(readOnly = true)
    public Page<Product> products(int page) {
        return products.findAll(PageRequest.of(Math.max(0, page), 30));
    }

    @Transactional(readOnly = true)
    public Product product(UUID id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Transactional
    public void moderateProduct(UUID adminId, UUID productId, ModerationStatus status, String reason) {
        String safeReason = requireReason(reason);
        User admin = user(adminId);
        Product product = product(productId);
        var before = product.getModerationStatus();
        product.changeModerationStatus(status);
        record(admin, "PRODUCT", productId, "MODERATE", before.name(), status.name(), safeReason);
    }

    @Transactional
    public void moderateMessage(UUID adminId, UUID messageId, boolean hidden, String reason) {
        String safeReason = requireReason(reason);
        User admin = user(adminId);
        Message message = messages.findById(messageId)
            .orElseThrow(() -> new NotFoundException("Message not found"));
        boolean before = message.isHidden();
        message.setHidden(hidden);
        record(admin, "MESSAGE", messageId, "MODERATE", String.valueOf(before),
            String.valueOf(hidden), safeReason);
    }

    @Transactional
    public List<Message> inspectConversation(UUID adminId, UUID conversationId, String reason) {
        String safeReason = requireReason(reason);
        User admin = user(adminId);
        List<Message> result = messages.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (result.isEmpty()) throw new NotFoundException("Conversation not found");
        audits.save(new AuditLog(admin, "ADMIN_CONVERSATION_INSPECTION", "CONVERSATION",
            conversationId, "SUCCESS", safeReason, "messageCount=" + result.size()));
        return result;
    }

    @Transactional(readOnly = true)
    public UUID conversationIdForMessage(UUID messageId) {
        return messages.findById(messageId)
            .map(message -> message.getConversation().getId())
            .orElseThrow(() -> new NotFoundException("Message not found"));
    }

    @Transactional(readOnly = true)
    public Page<Report> reportQueue(ReportStatus status, int page) {
        return reports.findByStatusOrderByCreatedAtAsc(
            status == null ? ReportStatus.OPEN : status, PageRequest.of(Math.max(0, page), 30));
    }

    @Transactional(readOnly = true)
    public Report report(UUID id) {
        return reports.findById(id).orElseThrow(() -> new NotFoundException("Report not found"));
    }

    @Transactional
    public void resolveReport(UUID adminId, UUID reportId, ReportStatus status, String reason) {
        if (status == null || status == ReportStatus.OPEN) throw new BusinessException("처리 상태를 선택해 주세요.");
        String safeReason = requireReason(reason);
        User admin = user(adminId);
        Report report = report(reportId);
        var before = report.getStatus();
        report.resolve(status, safeReason, admin);
        record(admin, "REPORT", reportId, "RESOLVE", before.name(), status.name(), safeReason);
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return categories.findAll();
    }

    @Transactional
    public void saveCategory(UUID adminId, UUID id, String name, boolean active, String reason) {
        String safeReason = requireReason(reason);
        String display = name == null ? "" : name.strip();
        if (display.length() < 2 || display.length() > 50) {
            throw new BusinessException("카테고리 이름은 2~50자여야 합니다.");
        }
        String normalized = IdentityNormalizer.nickname(display);
        User admin = user(adminId);
        Category category;
        String before;
        if (id == null) {
            if (categories.existsByNormalizedName(normalized)) throw new BusinessException("같은 카테고리가 있습니다.");
            category = categories.save(new Category(display, normalized));
            before = "NONE";
        } else {
            category = categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
            before = category.getName() + ":" + category.isActive();
            category.update(display, normalized, active);
        }
        record(admin, "CATEGORY", category.getId(), "SAVE", before,
            display + ":" + active, safeReason);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> audits(int page) {
        return audits.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), 50));
    }

    private String requireReason(String reason) {
        String value = reason == null ? "" : reason.strip().replaceAll("[\\r\\n\\t]", " ");
        if (value.length() < 5 || value.length() > 1000) {
            throw new BusinessException("관리 조치 사유는 5~1,000자로 입력해 주세요.");
        }
        return value;
    }

    private void record(User admin, String type, UUID target, String action, String before,
                        String after, String reason) {
        actions.save(new ModerationAction(admin, type, target, action, before, after, reason, "SUCCESS"));
        audits.save(new AuditLog(admin, action, type, target, "SUCCESS", reason,
            "before=" + before + ";after=" + after));
    }

    public record DashboardCounts(long users, long products, long openReports, long auditLogs) {
    }
}
