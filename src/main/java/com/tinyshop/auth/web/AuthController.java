package com.tinyshop.auth.web;

import com.tinyshop.auth.dto.ProfileForm;
import com.tinyshop.auth.dto.SignUpForm;
import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.auth.service.DuplicateIdentityException;
import com.tinyshop.auth.service.RegistrationService;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;

@Controller
public class AuthController {
    private final RegistrationService registration;
    private final UserService users;

    public AuthController(RegistrationService registration, UserService users) {
        this.registration = registration;
        this.users = users;
    }

    @InitBinder("form")
    void normalizeEmailBeforeValidation(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, "email", new StringTrimmerEditor(true));
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("form", new SignUpForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignUpForm form, BindingResult errors) {
        if (errors.hasErrors()) return "auth/signup";
        try {
            registration.register(form);
            return "redirect:/login?registered";
        } catch (DuplicateIdentityException exception) {
            String field = exception.getField() == DuplicateIdentityException.Field.NICKNAME
                ? "nickname" : "email";
            errors.rejectValue(field, "duplicate", exception.getMessage());
            return "auth/signup";
        } catch (BusinessException exception) {
            errors.reject("duplicate", exception.getMessage());
            return "auth/signup";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        model.addAttribute("user", users.require(principal.id()));
        return "user/dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        var user = users.require(principal.id());
        model.addAttribute("user", user);
        model.addAttribute("form", new ProfileForm(user.getNickname()));
        return "user/profile";
    }

    @PostMapping("/profile")
    public String profile(@AuthenticationPrincipal PlatformPrincipal principal,
                          @Valid @ModelAttribute("form") ProfileForm form,
                          BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("user", users.require(principal.id()));
            return "user/profile";
        }
        try {
            users.updateProfile(principal.id(), form);
            return "redirect:/profile?updated";
        } catch (BusinessException exception) {
            errors.rejectValue("nickname", "duplicate", exception.getMessage());
            model.addAttribute("user", users.require(principal.id()));
            return "user/profile";
        }
    }

    @PostMapping("/profile/withdraw")
    public String withdraw(@AuthenticationPrincipal PlatformPrincipal principal, HttpServletRequest request) {
        users.withdraw(principal.id());
        request.getSession(false).invalidate();
        return "redirect:/?withdrawn";
    }
}
