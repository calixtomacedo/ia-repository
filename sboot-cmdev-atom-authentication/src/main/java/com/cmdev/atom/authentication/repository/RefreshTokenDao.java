package com.cmdev.atom.authentication.repository;

import com.cmdev.atom.authentication.domain.model.RefreshToken;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenDao {

    @SqlUpdate("""
        INSERT INTO refresh_tokens (user_id, token, issued_at, expires_at, revoked, created_at)
        VALUES (:userId, :token, :issuedAt, :expiresAt, false, :createdAt)
    """)
    @GetGeneratedKeys("id")
    Long insert(
        @Bind("userId") Long userId,
        @Bind("token") String token,
        @Bind("issuedAt") Instant issuedAt,
        @Bind("expiresAt") Instant expiresAt,
        @Bind("createdAt") Instant createdAt
    );

    @SqlQuery("""
        SELECT id, user_id, token, issued_at, expires_at, revoked, revoked_at, created_at
        FROM refresh_tokens
        WHERE token = :token
    """)
    Optional<RefreshToken> findByToken(@Bind("token") String token);

    @SqlUpdate("""
        UPDATE refresh_tokens
        SET revoked = true, revoked_at = :revokedAt
        WHERE token = :token
    """)
    int revokeByToken(
        @Bind("token") String token,
        @Bind("revokedAt") Instant revokedAt
    );

    @SqlUpdate("""
        UPDATE refresh_tokens
        SET revoked = true, revoked_at = :revokedAt
        WHERE user_id = :userId
    """)
    int revokeAllByUserId(
        @Bind("userId") Long userId,
        @Bind("revokedAt") Instant revokedAt
    );

    @SqlUpdate("DELETE FROM refresh_tokens WHERE expires_at < :now OR revoked = true")
    int deleteExpiredOrRevoked(@Bind("now") Instant now);
}
