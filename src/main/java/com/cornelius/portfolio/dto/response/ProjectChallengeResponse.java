package com.cornelius.portfolio.dto.response;

public record ProjectChallengeResponse(
    Long id,
    String challenge,
    String lessonLearned
) {}
