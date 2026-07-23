package com.tinyshop.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessHandler
    implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {
    private final LoginAttemptService attempts;

    public AuthenticationSuccessHandler(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        attempts.succeeded(authentication.getName());
        response.sendRedirect("/dashboard");
    }
}
