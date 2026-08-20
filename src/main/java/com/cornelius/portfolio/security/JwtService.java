package com.cornelius.portfolio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        // Plain UTF-8 bytes, not Base64 — keeps the .env value a simple long
        // random string instead of requiring a separately-encoded secret.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret (JWT_SECRET) must be at least 32 characters (256 bits) for HS256. "
                    + "Current value is only " + keyBytes.length + " characters. "
                    + "Generate one with: openssl rand -hex 32"
            );
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(email)
            .claim(ROLE_CLAIM, role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
