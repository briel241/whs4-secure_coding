package com.tinyshop.config;

import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.wallet.domain.WalletAccount;
import com.tinyshop.wallet.domain.WalletLedgerEntry;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import com.tinyshop.wallet.repository.WalletLedgerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {
    private final AppProperties properties;
    private final UserRepository users;
    private final WalletAccountRepository wallets;
    private final WalletLedgerRepository ledger;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(AppProperties properties, UserRepository users,
                               WalletAccountRepository wallets, WalletLedgerRepository ledger,
                               PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.wallets = wallets;
        this.ledger = ledger;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        create(properties.demo().adminEmail(), "운영관리자", properties.demo().adminPassword(),
            UserRole.ADMIN, properties.demo().initialPoints());
        create("seller@example.invalid", "안전판매자", properties.demo().userPassword(),
            UserRole.USER, properties.demo().initialPoints());
        create("buyer@example.invalid", "꼼꼼구매자", properties.demo().userPassword(),
            UserRole.USER, properties.demo().initialPoints());
    }

    private void create(String rawEmail, String nickname, String password, UserRole role, long initialPoints) {
        String email = IdentityNormalizer.email(rawEmail);
        if (users.existsByNormalizedEmail(email)) return;
        User user = users.save(new User(email, email, nickname, IdentityNormalizer.nickname(nickname),
            passwordEncoder.encode(password), role));
        WalletAccount wallet = wallets.save(new WalletAccount(user, Math.max(0, initialPoints)));
        if (initialPoints > 0) ledger.save(WalletLedgerEntry.grant(wallet, initialPoints));
    }
}
