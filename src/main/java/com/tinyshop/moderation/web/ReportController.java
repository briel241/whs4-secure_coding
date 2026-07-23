package com.tinyshop.moderation.web;

import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.moderation.domain.ReportTargetType;
import com.tinyshop.moderation.domain.ReportType;
import com.tinyshop.moderation.dto.ReportForm;
import com.tinyshop.moderation.service.ReportService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports/new")
    public String form(@RequestParam ReportTargetType targetType, @RequestParam UUID targetId, Model model) {
        ReportForm form = new ReportForm();
        form.setTargetType(targetType);
        form.setTargetId(targetId);
        model.addAttribute("form", form);
        model.addAttribute("types", ReportType.values());
        return "moderation/report-form";
    }

    @PostMapping("/reports")
    public String create(@AuthenticationPrincipal PlatformPrincipal principal,
                         @Valid @ModelAttribute("form") ReportForm form,
                         BindingResult errors, Model model) {
        if (errors.hasErrors()) return invalid(model);
        try {
            reports.create(principal.id(), form);
            return "redirect:/reports/mine?created";
        } catch (BusinessException exception) {
            errors.reject("report", exception.getMessage());
            return invalid(model);
        }
    }

    @GetMapping("/reports/mine")
    public String mine(@AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        model.addAttribute("reports", reports.mine(principal.id()));
        return "moderation/mine";
    }

    private String invalid(Model model) {
        model.addAttribute("types", ReportType.values());
        return "moderation/report-form";
    }
}
