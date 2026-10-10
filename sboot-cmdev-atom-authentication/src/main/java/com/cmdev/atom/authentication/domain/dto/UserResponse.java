package com.cmdev.atom.authentication.domain.dto;

import com.cmdev.atom.authentication.domain.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados do usuário")
public record UserResponse(
        @Schema(description = "Identificador único do usuário", example = "1")
        Long id,

        @Schema(description = "Nome do usuário", example = "João Silva")
        String name,

        @Schema(description = "Email do usuário", example = "joao@email.com")
        String email,

        @Schema(description = "Papel/perfil do usuário", example = "USER")
        String role,

        @Schema(description = "Data e hora de criação no formato ISO-8601", example = "2026-10-02T10:00:00Z")
        //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "America/Sao_Paulo")
        Instant createdAt
) {
    public static UserResponse fromUser(User user) {
        String displayRole = user.role();
        if (displayRole != null && displayRole.startsWith("ROLE_")) {
            displayRole = displayRole.substring(5);
        }
        return new UserResponse(
                user.id(),
                user.name(),
                user.email(),
                displayRole != null && !displayRole.isBlank() ? displayRole : "USER",
                user.createdAt()
        );
    }
}
