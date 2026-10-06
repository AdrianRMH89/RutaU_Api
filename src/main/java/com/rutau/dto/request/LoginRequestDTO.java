package com.rutau.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password
) {}