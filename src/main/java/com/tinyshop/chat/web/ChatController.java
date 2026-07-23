package com.tinyshop.chat.web;

import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.chat.dto.MessageForm;
import com.tinyshop.chat.service.ChatService;
import com.tinyshop.common.exception.BusinessException;
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

@Controller
public class ChatController {
    private final ChatService chats;

    public ChatController(ChatService chats) {
        this.chats = chats;
    }

    @PostMapping("/products/{productId}/conversations")
    public String open(@PathVariable UUID productId,
                       @AuthenticationPrincipal PlatformPrincipal principal) {
        return "redirect:/conversations/" + chats.open(productId, principal.id()).getId();
    }

    @GetMapping("/conversations")
    public String list(@AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        model.addAttribute("conversations", chats.list(principal.id()));
        model.addAttribute("viewerId", principal.id());
        return "chat/list";
    }

    @GetMapping("/conversations/{id}")
    public String detail(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal,
                         Model model) {
        var conversation = chats.view(id, principal.id());
        model.addAttribute("conversation", conversation);
        model.addAttribute("counterparty", conversation.getBuyer().getId().equals(principal.id())
            ? conversation.getSeller() : conversation.getBuyer());
        model.addAttribute("messages", chats.visibleMessages(id, principal.id()));
        model.addAttribute("viewerId", principal.id());
        model.addAttribute("form", new MessageForm());
        return "chat/detail";
    }

    @PostMapping("/conversations/{id}/messages")
    public String send(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal,
                       @Valid @ModelAttribute("form") MessageForm form, BindingResult errors,
                       Model model) {
        if (errors.hasErrors()) return invalid(id, principal, model);
        try {
            chats.send(id, principal.id(), form.getBody());
            return "redirect:/conversations/" + id;
        } catch (BusinessException exception) {
            errors.reject("send", exception.getMessage());
            return invalid(id, principal, model);
        }
    }

    private String invalid(UUID id, PlatformPrincipal principal, Model model) {
        var conversation = chats.view(id, principal.id());
        model.addAttribute("conversation", conversation);
        model.addAttribute("counterparty", conversation.getBuyer().getId().equals(principal.id())
            ? conversation.getSeller() : conversation.getBuyer());
        model.addAttribute("messages", chats.visibleMessages(id, principal.id()));
        model.addAttribute("viewerId", principal.id());
        return "chat/detail";
    }
}
