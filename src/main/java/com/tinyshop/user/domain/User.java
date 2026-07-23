package com.tinyshop.user.domain;

import com.tinyshop.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "normalized_email", nullable = false, unique = true, length = 254)
    private String normalizedEmail;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "normalized_nickname", nullable = false, unique = true, length = 30)
    private String normalizedNickname;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Version
    private long version;

    protected User() {
    }

    public User(String email, String normalizedEmail, String nickname, String normalizedNickname,
                String passwordHash, UserRole role) {
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.nickname = nickname;
        this.normalizedNickname = normalizedNickname;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public void recordFailedLogin(int maxFailures, Instant lockUntil) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxFailures) this.lockedUntil = lockUntil;
    }

    public void clearLoginFailures() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void changeNickname(String nickname, String normalizedNickname) {
        this.nickname = nickname;
        this.normalizedNickname = normalizedNickname;
    }

    public String getEmail() {
        return email;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public String getNickname() {
        return nickname;
    }

    public String getNormalizedNickname() {
        return normalizedNickname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public long getVersion() {
        return version;
    }

    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE
            && (lockedUntil == null || lockedUntil.isBefore(Instant.now()));
    }
}
