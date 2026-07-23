package com.tinyshop.wallet.repository;

import com.tinyshop.wallet.domain.WalletLedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntry, UUID> {
    List<WalletLedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    @Query("select coalesce(sum(e.signedAmount), 0) from WalletLedgerEntry e where e.wallet.id = :walletId")
    long sumForWallet(@Param("walletId") UUID walletId);

    @Query("select coalesce(sum(e.signedAmount), 0) from WalletLedgerEntry e where e.transfer.id = :transferId")
    long sumForTransfer(@Param("transferId") UUID transferId);
}
