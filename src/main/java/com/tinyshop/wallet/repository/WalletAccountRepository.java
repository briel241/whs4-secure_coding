package com.tinyshop.wallet.repository;

import com.tinyshop.wallet.domain.WalletAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {
    Optional<WalletAccount> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletAccount w join fetch w.user where w.user.id in :userIds order by w.user.id")
    List<WalletAccount> lockAllByUserIds(@Param("userIds") Collection<UUID> userIds);
}
