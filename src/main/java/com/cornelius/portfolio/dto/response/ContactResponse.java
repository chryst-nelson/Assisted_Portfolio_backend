package com.cornelius.portfolio.dto.response;

import java.util.Map;

public record ContactResponse(
    String email,
    String linkedinUrl,
    String githubUrl,
    Map<String, String> otherLinks
) {}
