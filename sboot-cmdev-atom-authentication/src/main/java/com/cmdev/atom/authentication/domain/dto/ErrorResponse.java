package com.cmdev.atom.authentication.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Estrutura padrão de resposta de erro")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @Schema(description = "Código de status HTTP", example = "400")
        int status,

        @Schema(description = "Mensagem descritiva do erro", example = "Dados inválidos")
        String message,

        @Schema(description = "Data e hora do erro", example = "2026-10-05T11:00:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        Instant timestamp,

        @Schema(description = "Detalhes adicionais de validação")
        List<String> details
) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now(), List.of());
    }

    public static ErrorResponse of(int status, String message, List<String> details) {
        return new ErrorResponse(status, message, Instant.now(), details);
    }
}
