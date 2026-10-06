package com.rutau.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequestDTO(

        @NotBlank(message = "{validation.cancel.reason.required}")
        @Size(max = 255, message = "{validation.cancel.reason.max}")
        String reason
) {}
