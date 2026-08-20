package com.cornelius.portfolio.dto.response;

import java.time.LocalDate;

public record AchievementResponse(
    Long id,
    String title,
    String description,
    LocalDate date
) {}
