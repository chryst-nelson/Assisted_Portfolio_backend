package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SkillRequest(
    @NotBlank String name,
    @NotBlank String category,
    String proficiency,
    boolean currentlyLearning
) {}
