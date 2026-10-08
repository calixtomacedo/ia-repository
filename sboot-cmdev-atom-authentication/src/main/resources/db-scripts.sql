CREATE DATABASE auth_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE auth_db;

CREATE TABLE users (id BIGINT NOT NULL AUTO_INCREMENT,
                    uuid CHAR(36) NOT NULL,
                    name VARCHAR(150) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
                    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
                    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
                    last_login TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_users_uuid (uuid),
                    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE roles (id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(50) NOT NULL,
                    description VARCHAR(255),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_roles_name (name)
);

CREATE TABLE user_roles (user_id BIGINT NOT NULL,
                         role_id BIGINT NOT NULL,
                         PRIMARY KEY (user_id, role_id),
                         CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (id BIGINT NOT NULL AUTO_INCREMENT,
                             user_id BIGINT NOT NULL,
                             token VARCHAR(512) NOT NULL,
                             issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             expires_at TIMESTAMP NOT NULL,
                             revoked BOOLEAN NOT NULL DEFAULT FALSE,
                             revoked_at TIMESTAMP NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY(id),
                             UNIQUE KEY uk_refresh_token(token),
                             INDEX idx_refresh_user(user_id),
                             CONSTRAINT fk_refresh_token_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE login_audit (id BIGINT NOT NULL AUTO_INCREMENT,
                          user_id BIGINT NULL,
                          email VARCHAR(255),
                          ip_address VARCHAR(45),
                          user_agent VARCHAR(500),
                          success BOOLEAN NOT NULL,
                          login_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY(id),
                          INDEX idx_login_user(user_id),
                          CONSTRAINT fk_login_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL
);


CREATE TABLE logout_audit (id BIGINT NOT NULL AUTO_INCREMENT,
                           user_id BIGINT NOT NULL,
                           logout_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           access_token_hash VARCHAR(255),
                           PRIMARY KEY(id),
                           INDEX idx_logout_user(user_id),
                           CONSTRAINT fk_logout_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);


INSERT INTO roles(name, description)
VALUES('ROLE_ADMIN', 'Administrador do sistema'),
      ('ROLE_USER', 'Usuário padrão');

INSERT INTO users(uuid, name, email, password_hash)
VALUES(UUID(),
       'Administrador',
       'admin@empresa.com',
       '$2a$10$8NR4HEwt5RkJq2L4F6vE4eJ2SI6C7Je5A3M0baY1hJ7Vzj2p8Qj0q' -- new BCryptPasswordEncoder().encode("Admin@123")
      );

INSERT INTO user_roles(user_id, role_id)
VALUES(1, (SELECT id FROM roles WHERE name='ROLE_ADMIN' ));


CREATE INDEX idx_users_name ON users(name);
CREATE INDEX idx_users_last_login ON users(last_login);
CREATE INDEX idx_refresh_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_login_timestamp ON login_audit(login_timestamp);
CREATE INDEX idx_logout_timestamp ON logout_audit(logout_timestamp);