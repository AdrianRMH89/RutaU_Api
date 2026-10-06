package com.rutau.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "{validation.username.required}")
        @Size(min = 3, max = 50, message = "{validation.username.size}")
        String name,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        @Size(max = 150)
        String email,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 6, max = 100, message = "{validation.password.min}")
        String password,

        @NotBlank(message = "{validation.fullname.required}")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "{validation.university.required}")
        @Size(max = 150)
        String university,

        @Size(max = 100)
        String district,

        @Size(max = 30)
        String phone,

        @NotNull(message = "{validation.terms.required}")
        @AssertTrue(message = "{validation.terms.required}")
        Boolean acceptTerms
) {}