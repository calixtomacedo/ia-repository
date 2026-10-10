package com.cmdev.atom.authentication.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta simples com mensagem descritiva")
public record MessageResponse(
        @Schema(description = "Mensagem informativa", example = "Operação realizada com sucesso")
        String message
) {
}
