package com.rutau.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

// i18n - La API responde en español latinoamericano (es-419, por defecto) o en inglés (en-US)
// según el encabezado HTTP "Accept-Language" que envía el cliente.
@Configuration
public class I18nConfig {

    public static final Locale SPANISH_LATAM = Locale.forLanguageTag("es-419");
    public static final Locale ENGLISH_US = Locale.forLanguageTag("en-US");

    // "en", "en-US", "en-GB"... -> inglés (en-US). Cualquier otro idioma o sin encabezado -> es-419.
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                Locale requested = super.resolveLocale(request);
                return "en".equals(requested.getLanguage()) ? ENGLISH_US : SPANISH_LATAM;
            }
        };
        resolver.setSupportedLocales(List.of(SPANISH_LATAM, ENGLISH_US, Locale.ENGLISH));
        resolver.setDefaultLocale(SPANISH_LATAM);
        return resolver;
    }

    // Las validaciones (@NotBlank, @Size...) también toman sus mensajes de messages*.properties
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }
}
