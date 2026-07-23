package com.tinyshop.auth.service;

import com.tinyshop.common.exception.BusinessException;

public final class DuplicateIdentityException extends BusinessException {
    public enum Field {
        EMAIL,
        NICKNAME
    }

    private final Field field;

    public DuplicateIdentityException(Field field, String message) {
        super(message);
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
