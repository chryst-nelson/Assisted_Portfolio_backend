package com.cornelius.portfolio.dto.response;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresInMs
) {}
