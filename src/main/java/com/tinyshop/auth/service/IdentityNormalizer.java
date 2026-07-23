package com.tinyshop.auth.service;

import java.text.Normalizer;
import java.util.Locale;

public final class IdentityNormalizer {
    private IdentityNormalizer() {
    }

    public static String email(String value) {
        return value == null ? "" : Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
    }

    public static String nickname(String value) {
        return value == null ? "" : Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
    }

    public static String displayNickname(String value) {
        return value == null ? "" : Normalizer.normalize(value.strip(), Normalizer.Form.NFKC);
    }
}
