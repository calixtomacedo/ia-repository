package com.cmdev.atom.authentication.domain.model;

import java.time.Instant;

public record RefreshToken(
        Long id,
        Long userId,
        String token,
        Instant issuedAt,
        Instant expiresAt,
        boolean revoked,
        Instant revokedAt,
        Instant createdAt
) {
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
