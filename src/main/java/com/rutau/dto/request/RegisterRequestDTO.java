package com.rutau.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        String name,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "La universidad es obligatoria")
        @Size(max = 150)
        String university,

        @Size(max = 100)
        String district,

        @Size(max = 30)
        String phone,

        @NotNull(message = "Debes aceptar los Términos de Servicio")
        @AssertTrue(message = "Debes aceptar los Términos de Servicio")
        Boolean acceptTerms
) {}