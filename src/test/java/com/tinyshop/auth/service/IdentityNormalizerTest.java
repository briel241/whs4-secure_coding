package com.tinyshop.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityNormalizerTest {

    @Test
    void normalizesEmailAndUnicodeNickname() {
        assertThat(IdentityNormalizer.email("  USER@Example.COM  ")).isEqualTo("user@example.com");
        assertThat(IdentityNormalizer.nickname("  Ｔｉｎｙ  ")).isEqualTo("tiny");
    }
}
