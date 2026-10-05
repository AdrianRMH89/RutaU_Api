package com.rutau.dto.response;

import com.rutau.model.Role;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String fullName,
        String university,
        String district,
        Role role
) {}