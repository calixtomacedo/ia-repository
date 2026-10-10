package com.cmdev.atom.authentication.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum RolesEnum {

    ROLE_ADMIN(Boolean.TRUE, "Administrador do sistema"),
    ROLE_USER(Boolean.FALSE, "Usuário padrão");

    private static final Logger log = LoggerFactory.getLogger(RolesEnum.class);
    private final Boolean isAdmin;
    private final String description;

    RolesEnum(Boolean isAdmin, String description) {
        this.isAdmin = isAdmin;
        this.description = description;
    }

    public Boolean isAdmin() {
        return isAdmin;
    }

    public String getDescription() {
        return description;
    }

    public static RolesEnum fromRole(Boolean isAdmin) {
        if (isAdmin == null) {
            log.info("O tipo de usuário não veio na requisição. Com isso será definido como usuário {}", ROLE_USER.getDescription());
            return ROLE_USER;
        }

        for (RolesEnum role : values()) {
            if (role.isAdmin().equals(isAdmin)) {
                return role;
            }
        }
        return ROLE_USER;
    }


}