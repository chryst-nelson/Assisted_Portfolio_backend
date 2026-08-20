package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ExperienceRequest(
    @NotBlank String company,
    @NotBlank String role,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    String responsibilities,
    List<Long> technologyIds,
    List<String> achievements
) {}
