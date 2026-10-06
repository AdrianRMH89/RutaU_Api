package com.rutau.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// US19 - Cuando Hibernate crea la tabla notifications, agrega un CHECK con los tipos que existían
// en ese momento. Al agregar tipos nuevos (ACCOUNT_WARNING, ACCOUNT_SUSPENDED) ese CHECK impediría
// guardarlos, así que se elimina al iniciar. La columna sigue siendo un texto controlado por el enum.
@Slf4j
@Order(0)
@Component
@RequiredArgsConstructor
public class SchemaFixInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
        } catch (Exception ex) {
            log.warn("No se pudo actualizar la restricción de notifications: {}", ex.getMessage());
        }
    }
}
