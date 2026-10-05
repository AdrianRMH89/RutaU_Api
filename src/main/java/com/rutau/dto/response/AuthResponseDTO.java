package com.rutau.dto.response;

public record AuthResponseDTO(
        String token,
        String tokenType,
        UserResponseDTO user
) {}