package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
    @NotBlank String fullName,
    String title,
    String bio,
    String location,
    String interests,
    String careerGoals
) {}
