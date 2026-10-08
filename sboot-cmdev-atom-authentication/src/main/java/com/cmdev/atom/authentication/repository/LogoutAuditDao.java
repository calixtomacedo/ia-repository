package com.cmdev.atom.authentication.repository;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;

public interface LogoutAuditDao {

    @SqlUpdate("""
        INSERT INTO logout_audit (user_id, logout_timestamp, access_token_hash)
        VALUES (:userId, :timestamp, :accessTokenHash)
    """)
    void logLogout(
        @Bind("userId") Long userId,
        @Bind("timestamp") Instant timestamp,
        @Bind("accessTokenHash") String accessTokenHash
    );

    @SqlQuery("""
        SELECT COUNT(*) > 0
        FROM logout_audit
        WHERE access_token_hash = :tokenHash
    """)
    boolean isTokenRevoked(@Bind("tokenHash") String tokenHash);
}
