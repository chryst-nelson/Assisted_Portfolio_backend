package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AchievementRequest(
    @NotBlank String title,
    String description,
    LocalDate date,
    boolean verified
) {}
