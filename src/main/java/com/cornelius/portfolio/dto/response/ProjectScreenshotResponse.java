package com.cornelius.portfolio.dto.response;

public record ProjectScreenshotResponse(
    Long id,
    String url,
    String caption,
    int sortOrder
) {}
