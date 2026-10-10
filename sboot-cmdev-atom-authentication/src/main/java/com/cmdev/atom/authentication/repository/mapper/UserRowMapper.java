package com.cmdev.atom.authentication.repository.mapper;

import com.cmdev.atom.authentication.domain.model.User;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User map(ResultSet rs, StatementContext ctx) throws SQLException {
        Timestamp lastLoginTs = rs.getTimestamp("last_login");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        String role = "ROLE_USER";
        try {
            String r = rs.getString("role");
            if (r != null && !r.isBlank()) {
                role = r;
            }
        } catch (SQLException ignored) {
        }

        return new User(
                rs.getLong("id"),
                rs.getString("uuid"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getBoolean("enabled"),
                rs.getBoolean("account_non_expired"),
                rs.getBoolean("account_non_locked"),
                rs.getBoolean("credentials_non_expired"),
                lastLoginTs != null ? lastLoginTs.toInstant() : null,
                createdTs != null ? createdTs.toInstant() : null,
                updatedTs != null ? updatedTs.toInstant() : null,
                role
        );
    }
}
