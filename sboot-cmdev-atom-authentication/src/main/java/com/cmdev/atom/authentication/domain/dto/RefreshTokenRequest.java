package com.cmdev.atom.authentication.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição para renovação de token JWT")
public record RefreshTokenRequest(
    @Schema(description = "Token de atualização", example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank(message = "O refreshToken é obrigatório")
    String refreshToken
) {}
