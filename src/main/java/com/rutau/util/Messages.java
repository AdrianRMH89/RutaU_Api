package com.rutau.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

// i18n - Traduce una clave de mensaje al idioma de la petición (Accept-Language: es-419 o en-US).
// Si la clave no existe, devuelve el texto tal cual (así un texto normal también funciona).
@Component
@RequiredArgsConstructor
public class Messages {

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return get(LocaleContextHolder.getLocale(), key, args);
    }

    public String get(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
}
