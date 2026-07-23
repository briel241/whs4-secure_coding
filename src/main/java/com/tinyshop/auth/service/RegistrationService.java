package com.tinyshop.auth.service;

import com.tinyshop.auth.dto.SignUpForm;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.wallet.domain.WalletAccount;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final WalletAccountRepository wallets;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository users, WalletAccountRepository wallets,
                               PasswordEncoder passwordEncoder) {
        this.users = users;
        this.wallets = wallets;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(SignUpForm form) {
        String email = IdentityNormalizer.email(form.getEmail());
        String nickname = IdentityNormalizer.displayNickname(form.getNickname());
        String normalizedNickname = IdentityNormalizer.nickname(nickname);
        if (users.existsByNormalizedEmail(email)) {
            throw new DuplicateIdentityException(
                DuplicateIdentityException.Field.EMAIL, "이미 사용 중인 이메일입니다.");
        }
        if (users.existsByNormalizedNickname(normalizedNickname)) {
            throw new DuplicateIdentityException(
                DuplicateIdentityException.Field.NICKNAME, "이미 사용 중인 닉네임입니다.");
        }
        User user;
        try {
            user = users.saveAndFlush(new User(email, email, nickname, normalizedNickname,
                passwordEncoder.encode(form.getPassword()), UserRole.USER));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateConstraint(exception);
        }
        wallets.save(new WalletAccount(user, 0));
        return user;
    }

    private BusinessException duplicateConstraint(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        String normalizedMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("normalized_nickname")) {
            return new DuplicateIdentityException(
                DuplicateIdentityException.Field.NICKNAME, "이미 사용 중인 닉네임입니다.");
        }
        if (normalizedMessage.contains("normalized_email")) {
            return new DuplicateIdentityException(
                DuplicateIdentityException.Field.EMAIL, "이미 사용 중인 이메일입니다.");
        }
        return new BusinessException("이미 사용 중인 이메일 또는 닉네임입니다.");
    }
}
