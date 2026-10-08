package com.cmdev.atom.authentication.repository.mapper;

import com.cmdev.atom.authentication.domain.model.RefreshToken;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class RefreshTokenRowMapper implements RowMapper<RefreshToken> {

    @Override
    public RefreshToken map(ResultSet rs, StatementContext ctx) throws SQLException {
        Timestamp issuedTs = rs.getTimestamp("issued_at");
        Timestamp expiresTs = rs.getTimestamp("expires_at");
        Timestamp revokedTs = rs.getTimestamp("revoked_at");
        Timestamp createdTs = rs.getTimestamp("created_at");

        return new RefreshToken(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token"),
            issuedTs != null ? issuedTs.toInstant() : null,
            expiresTs != null ? expiresTs.toInstant() : null,
            rs.getBoolean("revoked"),
            revokedTs != null ? revokedTs.toInstant() : null,
            createdTs != null ? createdTs.toInstant() : null
        );
    }
}
