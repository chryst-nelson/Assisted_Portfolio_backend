package com.cornelius.portfolio.dto.response;

public record SkillResponse(
    Long id,
    String name,
    String category,
    String proficiency,
    boolean currentlyLearning
) {}
