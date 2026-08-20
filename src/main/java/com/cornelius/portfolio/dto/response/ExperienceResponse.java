package com.cornelius.portfolio.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ExperienceResponse(
    Long id,
    String company,
    String role,
    LocalDate startDate,
    LocalDate endDate,
    String responsibilities,
    List<SkillResponse> technologies,
    List<String> achievements
) {}
