package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
    @NotBlank @Size(max = 2000, message = "Message must be 2000 characters or fewer") String message,
    String mode
) {}
