package com.rutau.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger / OpenAPI - documentación interactiva en /swagger-ui.html
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI rutauOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RutaU API - Carpooling Universitario")
                        .version("v1.0.0")
                        .description("""
                                Web Services de RutaU: plataforma de carpooling para estudiantes universitarios.

                                **Cómo probar:** 1) `POST /api/auth/login` 2) copiar el `token` 3) botón **Authorize** \
                                (arriba a la derecha) y pegar el token.

                                **Idioma (i18n):** enviar `Accept-Language: en-US` para recibir los mensajes en inglés; \
                                por defecto se responde en español latinoamericano (`es-419`).

                                **Administrador:** `admin@rutau.edu.pe` / `admin12345`.""")
                        .contact(new Contact().name("Grupo 5 - 1ACC0236 Ingeniería de Software (UPC)"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                // Todos los endpoints usan el token JWT (excepto registro y login)
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .name(BEARER)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
