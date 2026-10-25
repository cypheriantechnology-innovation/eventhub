package com.eventhub.infrastructure.adapters.in.web.dto;

import com.eventhub.domain.model.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventRequest {

    @NotBlank(message = "O campo eventId é obrigatório.")
    @Size(max = 100, message = "O campo eventId deve ter no máximo 100 caracteres.")
    @Schema(description = "Identificador único do evento no chão de fábrica", example = "EVT-1001")
    private String eventId;

    @NotNull(message = "O campo eventType é obrigatório (PRODUCTION ou QUALITY).")
    @Schema(description = "Tipo de evento industrial", example = "PRODUCTION")
    private EventType eventType;

    @NotBlank(message = "O campo description é obrigatório.")
    @Size(max = 500, message = "O campo description deve ter no máximo 500 caracteres.")
    @Schema(description = "Descrição detalhada da ocorrência", example = "Linha de montagem A1 iniciada")
    private String description;

    @Schema(description = "Data e hora do evento (opcional, padrão: data/hora atual de recebimento)", example = "2026-05-20T10:00:00")
    private LocalDateTime timestamp;
}
