package com.tinyshop.config;

import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.wallet.domain.WalletAccount;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class AdminBootstrapInitializer implements ApplicationRunner {
    private final AppProperties properties;
    private final UserRepository users;
    private final WalletAccountRepository wallets;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapInitializer(AppProperties properties, UserRepository users,
                                     WalletAccountRepository wallets, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.wallets = wallets;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var config = properties.bootstrapAdmin();
        if (!config.enabled()) return;
        String email = IdentityNormalizer.email(config.email());
        String nickname = IdentityNormalizer.displayNickname(config.nickname());
        if (email.isBlank() || !email.contains("@") || config.password().length() < 12
            || config.password().length() > 128 || nickname.length() < 2 || nickname.length() > 30) {
            throw new IllegalStateException("Bootstrap admin settings are invalid");
        }
        users.findByNormalizedEmail(email).ifPresentOrElse(existing -> {
            if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("Bootstrap email already belongs to a non-admin user");
            }
        }, () -> {
            User admin = users.save(new User(email, email, nickname,
                IdentityNormalizer.nickname(nickname), passwordEncoder.encode(config.password()), UserRole.ADMIN));
            wallets.save(new WalletAccount(admin, 0));
        });
    }
}
