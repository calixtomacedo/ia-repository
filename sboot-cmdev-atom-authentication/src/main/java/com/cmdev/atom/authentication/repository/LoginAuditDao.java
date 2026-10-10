package com.cmdev.atom.authentication.repository;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;

public interface LoginAuditDao {

    @SqlUpdate("""
                INSERT INTO login_audit (user_id, email, ip_address, user_agent, success, login_timestamp)
                VALUES (:userId, :email, :ipAddress, :userAgent, :success, :timestamp)
            """)
    void logLogin(
            @Bind("userId") Long userId,
            @Bind("email") String email,
            @Bind("ipAddress") String ipAddress,
            @Bind("userAgent") String userAgent,
            @Bind("success") boolean success,
            @Bind("timestamp") Instant timestamp
    );
}
