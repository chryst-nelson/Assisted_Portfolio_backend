package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {}
