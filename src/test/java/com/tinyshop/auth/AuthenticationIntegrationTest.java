package com.tinyshop.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tinyshop.PostgresIntegrationTest;
import com.tinyshop.auth.dto.SignUpForm;
import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.user.domain.UserStatus;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

class AuthenticationIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired WalletAccountRepository wallets;
    @Autowired PasswordEncoder encoder;

    @Test
    void registrationNormalizesCreatesWalletAndDoesNotMassAssign() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/signup").with(csrf())
                .param("email", "  SAFE-" + suffix + "@Example.Invalid ")
                .param("nickname", "safe-" + suffix)
                .param("password", "very-long-safe-password")
                .param("passwordConfirm", "very-long-safe-password")
                .param("role", "ADMIN")
                .param("balance", "999999999"))
            .andExpect(status().is3xxRedirection());

        var user = users.findByNormalizedEmail("safe-" + suffix + "@example.invalid").orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(wallets.findByUserId(user.getId()).orElseThrow().getBalance()).isZero();
    }

    @Test
    void duplicateNormalizedIdentityIsRejectedAndHashesAreSalted() {
        String password = "same-password-for-both";
        var first = register("hash-a", password);
        var second = register("hash-b", password);
        assertThat(first.getPasswordHash()).isNotEqualTo(second.getPasswordHash());
        assertThat(encoder.matches(password, first.getPasswordHash())).isTrue();

        SignUpForm duplicate = new SignUpForm();
        duplicate.setEmail(" " + first.getEmail().toUpperCase() + " ");
        duplicate.setNickname("different-" + UUID.randomUUID().toString().substring(0, 6));
        duplicate.setPassword("another-secure-password");
        duplicate.setPasswordConfirm("another-secure-password");
        assertThatThrownBy(() -> registration.register(duplicate)).isInstanceOf(BusinessException.class);
    }

    @Test
    void duplicateNormalizedNicknameOnSignupMarksNicknameFieldInvalid() throws Exception {
        var existing = register("signup-nickname");
        String attemptedEmail = "new-" + UUID.randomUUID() + "@example.invalid";

        mvc.perform(post("/signup").with(csrf())
                .param("email", attemptedEmail)
                .param("nickname", " " + existing.getNickname().toUpperCase(Locale.ROOT) + " ")
                .param("password", "another-secure-password")
                .param("passwordConfirm", "another-secure-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("이미 사용 중인 닉네임입니다.")))
            .andExpect(content().string(containsString("class=\"input-error\"")))
            .andExpect(content().string(containsString("aria-invalid=\"true\"")));

        assertThat(users.findByNormalizedEmail(attemptedEmail)).isEmpty();
    }

    @Test
    void duplicateNormalizedNicknameOnProfileUpdateIsRejectedAndHighlighted() throws Exception {
        var current = register("profile-current");
        var existing = register("profile-taken");
        String originalNickname = current.getNickname();

        mvc.perform(post("/profile")
                .with(user(new PlatformPrincipal(current)))
                .with(csrf())
                .param("nickname", " " + existing.getNickname().toUpperCase(Locale.ROOT) + " "))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("이미 사용 중인 닉네임입니다.")))
            .andExpect(content().string(containsString("class=\"input-error\"")))
            .andExpect(content().string(containsString("aria-invalid=\"true\"")));

        assertThat(users.findById(current.getId()).orElseThrow().getNickname())
            .isEqualTo(originalNickname);
    }

    @Test
    void repeatedFailureLocksAccountAndSuspendedAccountCannotLogin() throws Exception {
        String password = "correct-password-123";
        var account = register("lock", password);
        for (int i = 0; i < 5; i++) {
            mvc.perform(formLogin().user(account.getEmail()).password("incorrect-password"))
                .andExpect(status().is3xxRedirection());
        }
        var locked = users.findById(account.getId()).orElseThrow();
        assertThat(locked.getLockedUntil()).isNotNull();
        mvc.perform(formLogin().user(account.getEmail()).password(password))
            .andExpect(status().is3xxRedirection());

        var suspended = register("suspended", password);
        suspended.changeStatus(UserStatus.SUSPENDED);
        users.saveAndFlush(suspended);
        mvc.perform(formLogin().user(suspended.getEmail()).password(password))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void logoutRequiresCsrf() throws Exception {
        var account = register("logout");
        mvc.perform(post("/logout").with(user(new PlatformPrincipal(account))))
            .andExpect(status().is4xxClientError());
        mvc.perform(post("/logout").with(user(new PlatformPrincipal(account))).with(csrf()))
            .andExpect(status().is3xxRedirection());
    }
}
