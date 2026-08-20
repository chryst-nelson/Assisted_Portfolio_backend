package com.cornelius.portfolio.dto.response;

import java.time.LocalDateTime;

public record ResumeResponse(
    String fileUrl,
    String version,
    LocalDateTime uploadedAt
) {}
