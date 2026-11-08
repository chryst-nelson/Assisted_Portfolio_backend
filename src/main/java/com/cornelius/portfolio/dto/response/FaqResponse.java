package com.cornelius.portfolio.dto.response;

public record FaqResponse(
    Long id,
    String question,
    String answer,
    String keywords,
    boolean enabled
) {}
