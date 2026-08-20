package com.cornelius.portfolio.dto.response;

import java.time.LocalDate;

public record CertificationResponse(
    Long id,
    String name,
    String provider,
    LocalDate issuedDate,
    String credentialUrl
) {}
