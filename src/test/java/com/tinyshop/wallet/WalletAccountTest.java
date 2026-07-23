package com.tinyshop.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.wallet.domain.WalletAccount;
import org.junit.jupiter.api.Test;

class WalletAccountTest {
    @Test
    void neverAllowsNegativeBalance() {
        User user = new User("x@example.invalid", "x@example.invalid", "닉네임", "닉네임",
            "hash", UserRole.USER);
        WalletAccount wallet = new WalletAccount(user, 100);
        wallet.debit(80);
        assertThat(wallet.getBalance()).isEqualTo(20);
        assertThatThrownBy(() -> wallet.debit(21)).isInstanceOf(IllegalArgumentException.class);
        assertThat(wallet.getBalance()).isEqualTo(20);
    }
}
