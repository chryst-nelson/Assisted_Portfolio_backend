package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ContactRequest(
    @NotBlank @Email String email,
    String linkedinUrl,
    String githubUrl,
    Map<String, String> otherLinks
) {}
