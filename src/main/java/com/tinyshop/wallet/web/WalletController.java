package com.tinyshop.wallet.web;

import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.wallet.dto.TransferConfirmForm;
import com.tinyshop.wallet.dto.TransferForm;
import com.tinyshop.wallet.service.WalletService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WalletController {
    private final WalletService wallets;

    public WalletController(WalletService wallets) {
        this.wallets = wallets;
    }

    @GetMapping("/wallet")
    public String wallet(@AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        model.addAttribute("wallet", wallets.wallet(principal.id()));
        model.addAttribute("reconciliation", wallets.reconcile(principal.id()));
        model.addAttribute("history", wallets.history(principal.id()).stream().limit(5).toList());
        model.addAttribute("viewerId", principal.id());
        return "wallet/index";
    }

    @GetMapping("/wallet/transfer")
    public String transferForm(Model model) {
        model.addAttribute("form", new TransferForm());
        return "wallet/transfer-form";
    }

    @PostMapping("/wallet/transfer-intents")
    public String prepare(@AuthenticationPrincipal PlatformPrincipal principal,
                          @Valid @ModelAttribute("form") TransferForm form, BindingResult errors) {
        if (errors.hasErrors()) return "wallet/transfer-form";
        try {
            return "redirect:/wallet/transfer-intents/" + wallets.prepare(principal.id(), form).getId()
                + "/confirm";
        } catch (BusinessException exception) {
            errors.reject("transfer", exception.getMessage());
            return "wallet/transfer-form";
        }
    }

    @GetMapping("/wallet/transfer-intents/{id}/confirm")
    public String confirm(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal,
                          Model model) {
        model.addAttribute("intent", wallets.pending(id, principal.id()));
        model.addAttribute("form", new TransferConfirmForm());
        return "wallet/transfer-confirm";
    }

    @PostMapping("/wallet/transfer-intents/{id}/confirm")
    public String execute(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal,
                          @Valid @ModelAttribute("form") TransferConfirmForm form,
                          BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("intent", wallets.pending(id, principal.id()));
            return "wallet/transfer-confirm";
        }
        try {
            return "redirect:/wallet/transfers/" + wallets.execute(
                id, principal.id(), form.getCurrentPassword()).getId();
        } catch (BusinessException exception) {
            errors.reject("transfer", exception.getMessage());
            model.addAttribute("intent", wallets.pending(id, principal.id()));
            return "wallet/transfer-confirm";
        }
    }

    @GetMapping("/wallet/transfers")
    public String history(@AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        model.addAttribute("history", wallets.history(principal.id()));
        model.addAttribute("viewerId", principal.id());
        return "wallet/history";
    }

    @GetMapping("/wallet/transfers/{id}")
    public String receipt(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal,
                          Model model) {
        model.addAttribute("transfer", wallets.receipt(id, principal.id()));
        model.addAttribute("viewerId", principal.id());
        return "wallet/receipt";
    }

    @GetMapping("/admin/transfers")
    public String adminTransfers(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", wallets.adminTransfers(page));
        return "admin/transfers";
    }
}
