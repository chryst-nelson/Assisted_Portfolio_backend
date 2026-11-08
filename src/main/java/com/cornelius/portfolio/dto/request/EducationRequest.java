package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record EducationRequest(
    @NotBlank String institution,
    String degree,
    String course,
    String details,
    LocalDate startDate,
    LocalDate endDate
) {}
