package com.cornelius.portfolio.dto.response;

public record ProjectSummaryResponse(
    Long id,
    String name,
    String slug,
    String description,
    String category,
    String githubUrl,
    String liveUrl
) {}
