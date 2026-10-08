package com.cmdev.atom.authentication.domain.model;

import java.time.Instant;

public record User(
    Long id,
    String uuid,
    String name,
    String email,
    String passwordHash,
    boolean enabled,
    boolean accountNonExpired,
    boolean accountNonLocked,
    boolean credentialsNonExpired,
    Instant lastLogin,
    Instant createdAt,
    Instant updatedAt,
    String role
) {
    public User withId(Long newId) {
        return new User(
            newId,
            this.uuid,
            this.name,
            this.email,
            this.passwordHash,
            this.enabled,
            this.accountNonExpired,
            this.accountNonLocked,
            this.credentialsNonExpired,
            this.lastLogin,
            this.createdAt,
            this.updatedAt,
            this.role
        );
    }
}
