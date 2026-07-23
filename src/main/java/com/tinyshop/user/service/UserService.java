package com.tinyshop.user.service;

import com.tinyshop.auth.dto.ProfileForm;
import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserStatus;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.wallet.domain.WalletStatus;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final WalletAccountRepository wallets;
    private final SessionRegistry sessions;

    public UserService(UserRepository users, WalletAccountRepository wallets, SessionRegistry sessions) {
        this.users = users;
        this.wallets = wallets;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public User require(java.util.UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public void updateProfile(java.util.UUID id, ProfileForm form) {
        User user = require(id);
        String nickname = IdentityNormalizer.displayNickname(form.getNickname());
        String normalized = IdentityNormalizer.nickname(nickname);
        users.findByNormalizedNickname(normalized)
            .filter(found -> !found.getId().equals(id))
            .ifPresent(found -> {
                throw new BusinessException("이미 사용 중인 닉네임입니다.");
        });
        try {
            user.changeNickname(nickname, normalized);
            users.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("이미 사용 중인 닉네임입니다.");
        }
    }

    @Transactional
    public void withdraw(java.util.UUID id) {
        User user = require(id);
        user.changeStatus(UserStatus.WITHDRAWN);
        wallets.findByUserId(id).ifPresent(wallet -> wallet.changeStatus(WalletStatus.CLOSED));
        expireSessions(id);
    }

    public void expireSessions(java.util.UUID userId) {
        sessions.getAllPrincipals().stream()
            .filter(PlatformPrincipal.class::isInstance)
            .map(PlatformPrincipal.class::cast)
            .filter(principal -> principal.id().equals(userId))
            .flatMap(principal -> sessions.getAllSessions(principal, false).stream())
            .forEach(session -> session.expireNow());
    }
}
