package com.cornelius.portfolio.dto.response;

import java.time.LocalDate;

public record EducationResponse(
    Long id,
    String institution,
    String degree,
    String course,
    String details,
    LocalDate startDate,
    LocalDate endDate
) {}
