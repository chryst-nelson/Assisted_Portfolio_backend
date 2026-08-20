package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResumeUploadRequest(
    @NotBlank String fileUrl,
    String version
) {}
