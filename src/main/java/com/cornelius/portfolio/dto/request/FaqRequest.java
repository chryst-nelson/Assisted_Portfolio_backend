package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqRequest(
    @NotBlank @Size(max = 500) String question,
    @NotBlank String answer,
    @Size(max = 500) String keywords,
    boolean enabled
) {}
