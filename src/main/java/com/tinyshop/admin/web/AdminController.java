package com.tinyshop.admin.web;

import com.tinyshop.admin.service.AdminService;
import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.moderation.domain.ReportStatus;
import com.tinyshop.moderation.domain.ReportTargetType;
import com.tinyshop.product.domain.ModerationStatus;
import com.tinyshop.user.domain.UserStatus;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("counts", admin.counts());
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(required = false) String query,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", admin.users(query, page));
        model.addAttribute("query", query);
        return "admin/users";
    }

    @GetMapping("/admin/users/{id}")
    public String user(@PathVariable UUID id, Model model) {
        model.addAttribute("user", admin.user(id));
        model.addAttribute("statuses", UserStatus.values());
        return "admin/user-detail";
    }

    @PostMapping("/admin/users/{id}/status")
    public String userStatus(@PathVariable UUID id, @RequestParam UserStatus status,
                             @RequestParam String reason,
                             @AuthenticationPrincipal PlatformPrincipal principal,
                             RedirectAttributes redirect) {
        return action(() -> admin.changeUserStatus(principal.id(), id, status, reason),
            "/admin/users/" + id, redirect);
    }

    @GetMapping("/admin/products")
    public String products(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", admin.products(page));
        model.addAttribute("statuses", ModerationStatus.values());
        return "admin/products";
    }

    @GetMapping("/admin/products/{id}")
    public String productDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("product", admin.product(id));
        model.addAttribute("statuses", ModerationStatus.values());
        return "admin/product-detail";
    }

    @PostMapping("/admin/products/{id}/moderation")
    public String product(@PathVariable UUID id, @RequestParam ModerationStatus status,
                          @RequestParam String reason,
                          @AuthenticationPrincipal PlatformPrincipal principal,
                          RedirectAttributes redirect) {
        return action(() -> admin.moderateProduct(principal.id(), id, status, reason),
            "/admin/products/" + id, redirect);
    }

    @GetMapping("/admin/reports")
    public String reports(@RequestParam(defaultValue = "OPEN") ReportStatus status,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", admin.reportQueue(status, page));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ReportStatus.values());
        return "admin/reports";
    }

    @GetMapping("/admin/reports/{id}")
    public String report(@PathVariable UUID id, Model model) {
        var report = admin.report(id);
        model.addAttribute("report", report);
        if (report.getTargetType() == ReportTargetType.MESSAGE) {
            model.addAttribute("messageConversationId",
                admin.conversationIdForMessage(report.getTargetId()));
        }
        model.addAttribute("statuses", ReportStatus.values());
        return "admin/report-detail";
    }

    @PostMapping("/admin/reports/{id}")
    public String reportResolve(@PathVariable UUID id, @RequestParam ReportStatus status,
                                @RequestParam String reason,
                                @AuthenticationPrincipal PlatformPrincipal principal,
                                RedirectAttributes redirect) {
        return action(() -> admin.resolveReport(principal.id(), id, status, reason),
            "/admin/reports/" + id, redirect);
    }

    @PostMapping("/admin/messages/{id}/moderation")
    public String message(@PathVariable UUID id, @RequestParam boolean hidden,
                          @RequestParam String reason, @RequestParam UUID conversationId,
                          @AuthenticationPrincipal PlatformPrincipal principal,
                          RedirectAttributes redirect) {
        return action(() -> admin.moderateMessage(principal.id(), id, hidden, reason),
            "/admin/messages/inspect?conversationId=" + conversationId, redirect);
    }

    @GetMapping("/admin/messages")
    public String messageLookup() {
        return "admin/message-lookup";
    }

    @GetMapping("/admin/messages/inspect")
    public String inspectReason(@RequestParam UUID conversationId, Model model) {
        model.addAttribute("conversationId", conversationId);
        return "admin/inspect-reason";
    }

    @PostMapping("/admin/messages/inspect")
    public String inspect(@RequestParam UUID conversationId, @RequestParam String reason,
                          @AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        try {
            model.addAttribute("messages",
                admin.inspectConversation(principal.id(), conversationId, reason));
            model.addAttribute("conversationId", conversationId);
            return "admin/messages";
        } catch (BusinessException exception) {
            model.addAttribute("conversationId", conversationId);
            model.addAttribute("error", exception.getMessage());
            return "admin/inspect-reason";
        }
    }

    @GetMapping("/admin/categories")
    public String categories(Model model) {
        model.addAttribute("categories", admin.categories());
        return "admin/categories";
    }

    @PostMapping("/admin/categories")
    public String category(@RequestParam(required = false) UUID id, @RequestParam String name,
                           @RequestParam(defaultValue = "false") boolean active,
                           @RequestParam String reason,
                           @AuthenticationPrincipal PlatformPrincipal principal,
                           RedirectAttributes redirect) {
        return action(() -> admin.saveCategory(principal.id(), id, name, active, reason),
            "/admin/categories", redirect);
    }

    @GetMapping("/admin/audit")
    public String audit(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", admin.audits(page));
        return "admin/audit";
    }

    private String action(Runnable action, String redirectTo, RedirectAttributes redirect) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", "관리 조치를 기록하고 적용했습니다.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:" + redirectTo;
    }
}
