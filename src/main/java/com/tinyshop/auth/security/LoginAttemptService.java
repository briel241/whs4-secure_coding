package com.tinyshop.auth.security;

import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.config.AppProperties;
import com.tinyshop.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {
    private final UserRepository users;
    private final AppProperties properties;

    public LoginAttemptService(UserRepository users, AppProperties properties) {
        this.users = users;
        this.properties = properties;
    }

    @Transactional
    public void failed(String email) {
        users.findByNormalizedEmail(IdentityNormalizer.email(email)).ifPresent(user ->
            user.recordFailedLogin(properties.security().loginMaxFailures(),
                Instant.now().plus(properties.security().loginLockMinutes(), ChronoUnit.MINUTES)));
    }

    @Transactional
    public void succeeded(String email) {
        users.findByNormalizedEmail(IdentityNormalizer.email(email))
            .ifPresent(user -> user.clearLoginFailures());
    }
}
