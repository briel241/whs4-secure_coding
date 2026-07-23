package com.tinyshop.wallet.repository;

import com.tinyshop.wallet.domain.TransferIntent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TransferIntentRepository extends JpaRepository<TransferIntent, UUID> {
    Optional<TransferIntent> findByIdAndSenderId(UUID id, UUID senderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select i from TransferIntent i
        join fetch i.sender join fetch i.recipient
        where i.id = :id and i.sender.id = :senderId
        """)
    Optional<TransferIntent> lockOwned(@Param("id") UUID id, @Param("senderId") UUID senderId);
}
