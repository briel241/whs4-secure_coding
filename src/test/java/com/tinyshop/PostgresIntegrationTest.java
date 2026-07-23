package com.tinyshop;

import com.tinyshop.auth.dto.SignUpForm;
import com.tinyshop.auth.service.RegistrationService;
import com.tinyshop.user.domain.User;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17.10-alpine")
            .withDatabaseName("tinyshop_test")
            .withUsername("tinyshop")
            .withPassword("test-only-password");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected RegistrationService registration;

    protected User register(String prefix) {
        return register(prefix, "Correct-Horse-" + UUID.randomUUID());
    }

    protected User register(String prefix, String password) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        SignUpForm form = new SignUpForm();
        form.setEmail(prefix + "-" + suffix + "@example.invalid");
        form.setNickname(prefix + "-" + suffix);
        form.setPassword(password);
        form.setPasswordConfirm(password);
        return registration.register(form);
    }
}
