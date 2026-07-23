package com.tinyshop.auth.security;

import com.tinyshop.user.domain.User;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class PlatformPrincipal implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String nickname;
    private final String role;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public PlatformPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getNormalizedEmail();
        this.passwordHash = user.getPasswordHash();
        this.nickname = user.getNickname();
        this.role = user.getRole().name();
        this.enabled = user.getStatus() == com.tinyshop.user.domain.UserStatus.ACTIVE;
        this.accountNonLocked = user.canAuthenticate();
    }

    public UUID id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
}
