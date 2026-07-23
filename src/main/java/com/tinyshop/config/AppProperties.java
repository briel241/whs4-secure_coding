package com.tinyshop.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Upload upload, Security security, Message message, Demo demo,
                            BootstrapAdmin bootstrapAdmin) {

    public record Upload(Path directory, long maxFileBytes, long maxPixels, int maxWidth, int maxHeight) {
    }

    public record Security(int loginMaxFailures, int loginLockMinutes) {
    }

    public record Message(int rateLimitCount, int rateLimitWindowSeconds) {
    }

    public record Demo(boolean enabled, long initialPoints, String adminEmail, String adminPassword,
                       String userPassword) {
        public Demo {
            if (adminEmail == null) adminEmail = "admin@example.invalid";
            if (adminPassword == null) adminPassword = "change-this-demo-password";
            if (userPassword == null) userPassword = "change-this-demo-password";
        }
    }

    public record BootstrapAdmin(boolean enabled, String email, String nickname, String password) {
        public BootstrapAdmin {
            if (email == null) email = "";
            if (nickname == null) nickname = "관리자";
            if (password == null) password = "";
        }
    }
}
