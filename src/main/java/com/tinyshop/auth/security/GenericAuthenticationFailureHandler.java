package com.tinyshop.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class GenericAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final LoginAttemptService attempts;

    public GenericAuthenticationFailureHandler(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        attempts.failed(request.getParameter("username"));
        response.sendRedirect("/login?error");
    }
}
