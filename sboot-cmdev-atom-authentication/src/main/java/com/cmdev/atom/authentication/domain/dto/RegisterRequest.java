package com.cmdev.atom.authentication.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisição para cadastro de novo usuário")
public record RegisterRequest(
    @Schema(description = "Nome do usuário", example = "João Silva")
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 255, message = "O nome deve ter entre 2 e 255 caracteres")
    String name,

    @Schema(description = "Email do usuário", example = "joao@email.com")
    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email informado é inválido")
    String email,

    @Schema(description = "Senha do usuário", example = "Senha@123")
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, max = 100, message = "A senha deve ter no mínimo 6 caracteres")
    String password
) {}
