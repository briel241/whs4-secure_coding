package com.tinyshop.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tinyshop.PostgresIntegrationTest;
import com.tinyshop.admin.repository.AuditLogRepository;
import com.tinyshop.admin.service.AdminService;
import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.product.domain.ItemCondition;
import com.tinyshop.product.domain.ModerationStatus;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.product.repository.CategoryRepository;
import com.tinyshop.product.service.ProductService;
import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserRole;
import com.tinyshop.user.domain.UserStatus;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.wallet.domain.WalletAccount;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

class AdminIntegrationTest extends PostgresIntegrationTest {
    @Autowired AdminService admin;
    @Autowired ProductService products;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired WalletAccountRepository wallets;
    @Autowired AuditLogRepository audits;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate transactions;

    @Test
    void regularUserCannotAccessAdminRoute() throws Exception {
        var regular = register("not-admin");
        mvc.perform(get("/admin").with(user(new PlatformPrincipal(regular))))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminDashboardExposesEveryManagementArea() throws Exception {
        var administrator = createAdmin();
        mvc.perform(get("/admin").with(user(new PlatformPrincipal(administrator))))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/admin/users")))
            .andExpect(content().string(containsString("/admin/products")))
            .andExpect(content().string(containsString("/admin/messages")))
            .andExpect(content().string(containsString("/admin/reports")))
            .andExpect(content().string(containsString("/admin/categories")))
            .andExpect(content().string(containsString("/admin/transfers")))
            .andExpect(content().string(containsString("/admin/audit")));
    }

    @Test
    void productHideRemovesDirectPublicAccessAndCreatesAudit() throws Exception {
        User administrator = createAdmin();
        User seller = register("moderated-seller");
        var product = products.create(seller.getId(), form(), List.of());
        mvc.perform(get("/admin/products/" + product.getId())
                .with(user(new PlatformPrincipal(administrator))))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(product.getTitle())));
        long before = audits.count();
        admin.moderateProduct(administrator.getId(), product.getId(), ModerationStatus.HIDDEN,
            "금지 품목 조사 결과");
        assertThatThrownBy(() -> products.detail(product.getId(), null)).isInstanceOf(NotFoundException.class);
        assertThat(audits.count()).isEqualTo(before + 1);
        assertThatThrownBy(() -> admin.moderateProduct(administrator.getId(), product.getId(),
            ModerationStatus.VISIBLE, "")).isInstanceOf(BusinessException.class);
    }

    @Test
    void lastActiveAdminCannotBeBlockedAndSuspendedUserWalletIsFrozen() {
        User administrator = createAdmin();
        transactions.executeWithoutResult(status -> users.findAll().stream()
            .filter(existing -> existing.getRole() == UserRole.ADMIN)
            .filter(existing -> !existing.getId().equals(administrator.getId()))
            .forEach(existing -> existing.changeStatus(UserStatus.BLOCKED)));
        assertThatThrownBy(() -> admin.changeUserStatus(administrator.getId(), administrator.getId(),
            UserStatus.BLOCKED, "마지막 관리자 차단 시도")).isInstanceOf(BusinessException.class);

        User target = register("suspend-target");
        admin.changeUserStatus(administrator.getId(), target.getId(), UserStatus.SUSPENDED,
            "반복적인 이용 정책 위반");
        assertThat(users.findById(target.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(wallets.findByUserId(target.getId()).orElseThrow().getStatus())
            .isEqualTo(com.tinyshop.wallet.domain.WalletStatus.FROZEN);
    }

    private User createAdmin() {
        return transactions.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            String email = "admin-" + suffix + "@example.invalid";
            User administrator = users.save(new User(email, email, "관리자-" + suffix,
                IdentityNormalizer.nickname("관리자-" + suffix), encoder.encode("admin-password-123"),
                UserRole.ADMIN));
            wallets.save(new WalletAccount(administrator, 0));
            return administrator;
        });
    }

    private ProductForm form() {
        ProductForm form = new ProductForm();
        form.setTitle("관리 대상 상품");
        form.setDescription("설명");
        form.setPrice(100);
        form.setCategoryId(categories.findByActiveTrueOrderByName().getFirst().getId());
        form.setItemCondition(ItemCondition.GOOD);
        form.setTradeMethod(TradeMethod.DIRECT);
        form.setSaleStatus(SaleStatus.ON_SALE);
        return form;
    }
}
