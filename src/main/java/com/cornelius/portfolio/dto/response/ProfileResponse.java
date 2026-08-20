package com.cornelius.portfolio.dto.response;

import java.time.LocalDateTime;

public record ProfileResponse(
    Long id,
    String fullName,
    String title,
    String bio,
    String location,
    String interests,
    String careerGoals,
    LocalDateTime updatedAt
) {}
