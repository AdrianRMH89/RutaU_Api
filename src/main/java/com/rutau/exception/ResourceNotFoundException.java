package com.rutau.exception;

import lombok.Getter;

// El mensaje es una CLAVE de i18n (ver resources/i18n/messages*.properties).
// args son los valores que reemplazan {0}, {1}... dentro del mensaje traducido.
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final Object[] args;

    public ResourceNotFoundException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }
}
