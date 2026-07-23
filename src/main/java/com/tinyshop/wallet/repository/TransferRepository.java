package com.tinyshop.wallet.repository;

import com.tinyshop.wallet.domain.Transfer;
import com.tinyshop.wallet.domain.TransferStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    Optional<Transfer> findBySenderIdAndIdempotencyKey(UUID senderId, UUID idempotencyKey);

    @Query("""
        select t from Transfer t join fetch t.sender join fetch t.recipient
        where t.sender.id = :userId or t.recipient.id = :userId
        order by t.completedAt desc
        """)
    List<Transfer> findHistory(@Param("userId") UUID userId);

    @Query("""
        select coalesce(sum(t.amount), 0) from Transfer t
        where t.sender.id = :senderId and t.status = :status
          and t.completedAt >= :from and t.completedAt < :to
        """)
    long sumSent(@Param("senderId") UUID senderId, @Param("status") TransferStatus status,
                 @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select t from Transfer t join fetch t.sender join fetch t.recipient
        where t.id = :id and (t.sender.id = :userId or t.recipient.id = :userId)
        """)
    Optional<Transfer> findVisible(@Param("id") UUID id, @Param("userId") UUID userId);

    Page<Transfer> findAllByOrderByCompletedAtDesc(Pageable pageable);
}
