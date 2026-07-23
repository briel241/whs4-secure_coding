package com.tinyshop.wallet.domain;

import com.tinyshop.common.domain.BaseEntity;
import com.tinyshop.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "wallet_accounts")
public class WalletAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private long balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    @Version
    private long version;

    protected WalletAccount() {
    }

    public WalletAccount(User user, long balance) {
        this.user = user;
        this.balance = balance;
        this.status = WalletStatus.ACTIVE;
    }

    public void debit(long amount) {
        if (amount <= 0 || balance < amount) throw new IllegalArgumentException("Insufficient balance");
        balance -= amount;
    }

    public void credit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Invalid credit");
        balance = Math.addExact(balance, amount);
    }

    public void changeStatus(WalletStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public long getBalance() {
        return balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
