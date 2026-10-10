package com.cmdev.atom.authentication.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisição para atualização de usuário")
public record UpdateUserRequest(
        @Schema(description = "Nome do usuário", example = "João Silva Atualizado")
        @Size(min = 2, max = 255, message = "O nome deve ter entre 2 e 255 caracteres")
        String name,

        @Schema(description = "Email do usuário", example = "joao.novo@email.com")
        @Email(message = "O email informado é inválido")
        String email
) {
}
