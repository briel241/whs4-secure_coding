package com.tinyshop.wallet.domain;

import com.tinyshop.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet_ledger_entries")
public class WalletLedgerEntry extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletAccount wallet;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;
    @Column(name = "signed_amount", nullable = false)
    private long signedAmount;
    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType entryType;

    protected WalletLedgerEntry() {
    }

    public WalletLedgerEntry(WalletAccount wallet, Transfer transfer, long signedAmount,
                             long balanceAfter, LedgerEntryType entryType) {
        this.wallet = wallet;
        this.transfer = transfer;
        this.signedAmount = signedAmount;
        this.balanceAfter = balanceAfter;
        this.entryType = entryType;
    }

    public static WalletLedgerEntry grant(WalletAccount wallet, long amount) {
        return new WalletLedgerEntry(wallet, null, amount, wallet.getBalance(), LedgerEntryType.GRANT);
    }

    public WalletAccount getWallet() { return wallet; }
    public Transfer getTransfer() { return transfer; }
    public long getSignedAmount() { return signedAmount; }
    public long getBalanceAfter() { return balanceAfter; }
    public LedgerEntryType getEntryType() { return entryType; }
}
