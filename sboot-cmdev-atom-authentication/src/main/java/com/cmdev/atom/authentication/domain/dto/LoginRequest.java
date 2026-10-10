package com.cmdev.atom.authentication.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição para autenticação de usuário")
public record LoginRequest(
        @Schema(description = "Email do usuário", example = "admin@empresa.com")
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email informado é inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "Senha@123")
        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
