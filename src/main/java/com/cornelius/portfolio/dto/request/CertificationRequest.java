package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CertificationRequest(
    @NotBlank String name,
    String provider,
    LocalDate issuedDate,
    String credentialUrl
) {}
