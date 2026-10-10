package com.cmdev.atom.authentication.repository;

import com.cmdev.atom.authentication.domain.model.User;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserDao {

    @SqlUpdate("""
                INSERT INTO users (uuid, name, email, password_hash, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at)
                VALUES (:uuid, :name, :email, :passwordHash, true, true, true, true, :createdAt, :updatedAt)
            """)
    @GetGeneratedKeys("id")
    Long insert(
            @Bind("uuid") String uuid,
            @Bind("name") String name,
            @Bind("email") String email,
            @Bind("passwordHash") String passwordHash,
            @Bind("createdAt") Instant createdAt,
            @Bind("updatedAt") Instant updatedAt
    );

    @SqlUpdate("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT :userId, id FROM roles WHERE name = :roleName
            """)
    int assignRole(
            @Bind("userId") Long userId,
            @Bind("roleName") String roleName
    );

    @SqlQuery("""
                SELECT u.id, u.uuid, u.name, u.email, u.password_hash, u.enabled,
                       u.account_non_expired, u.account_non_locked, u.credentials_non_expired,
                       u.last_login, u.created_at, u.updated_at,
                       COALESCE(
                         (SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = u.id LIMIT 1),
                         'ROLE_USER'
                       ) AS role
                FROM users u
                WHERE u.id = :id
            """)
    Optional<User> findById(@Bind("id") Long id);

    @SqlQuery("""
                SELECT u.id, u.uuid, u.name, u.email, u.password_hash, u.enabled,
                       u.account_non_expired, u.account_non_locked, u.credentials_non_expired,
                       u.last_login, u.created_at, u.updated_at,
                       COALESCE(
                         (SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = u.id LIMIT 1),
                         'ROLE_USER'
                       ) AS role
                FROM users u
                WHERE u.email = :email
            """)
    Optional<User> findByEmail(@Bind("email") String email);

    @SqlQuery("""
                SELECT u.id, u.uuid, u.name, u.email, u.password_hash, u.enabled,
                       u.account_non_expired, u.account_non_locked, u.credentials_non_expired,
                       u.last_login, u.created_at, u.updated_at,
                       COALESCE(
                         (SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = u.id LIMIT 1),
                         'ROLE_USER'
                       ) AS role
                FROM users u
                ORDER BY u.id ASC
            """)
    List<User> findAll();

    @SqlUpdate("""
                UPDATE users
                SET name = :name, email = :email, updated_at = :updatedAt
                WHERE id = :id
            """)
    int update(
            @Bind("id") Long id,
            @Bind("name") String name,
            @Bind("email") String email,
            @Bind("updatedAt") Instant updatedAt
    );

    @SqlUpdate("""
                UPDATE users
                SET last_login = :lastLogin
                WHERE id = :id
            """)
    int updateLastLogin(
            @Bind("id") Long id,
            @Bind("lastLogin") Instant lastLogin
    );

    @SqlUpdate("DELETE FROM users WHERE id = :id")
    int deleteById(@Bind("id") Long id);

    @SqlQuery("SELECT COUNT(*) > 0 FROM users WHERE email = :email")
    boolean existsByEmail(@Bind("email") String email);

    @SqlQuery("SELECT COUNT(*) > 0 FROM users WHERE email = :email AND id <> :id")
    boolean existsByEmailAndIdNot(@Bind("email") String email, @Bind("id") Long id);
}
