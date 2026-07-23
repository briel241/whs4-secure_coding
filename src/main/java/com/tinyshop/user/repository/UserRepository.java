package com.tinyshop.user.repository;

import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.user.domain.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByNormalizedEmail(String normalizedEmail);
    Optional<User> findByNormalizedNickname(String normalizedNickname);
    boolean existsByNormalizedEmail(String normalizedEmail);
    boolean existsByNormalizedNickname(String normalizedNickname);
    long countByRoleAndStatus(UserRole role, UserStatus status);
    Page<User> findByNicknameContainingIgnoreCase(String keyword, Pageable pageable);
}
