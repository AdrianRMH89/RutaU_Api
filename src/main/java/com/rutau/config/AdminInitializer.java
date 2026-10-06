package com.rutau.config;

import com.rutau.model.Role;
import com.rutau.model.User;
import com.rutau.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// US17-US19 - Crea la cuenta de ADMINISTRADOR al iniciar la app (si aún no existe).
// Nadie puede registrarse como admin desde /api/auth/register: solo existe esta cuenta.
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${rutau.admin.email}")
    private String adminEmail;

    @Value("${rutau.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = new User();
        admin.setName("admin");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFullName("Administrador RutaU");
        admin.setUniversity("RutaU");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Cuenta de administrador creada: {}", adminEmail);
    }
}
