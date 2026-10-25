package com.eventhub.infrastructure.adapters.in.web.dto;

import com.eventhub.domain.model.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registro de trilha de auditoria imutável")
public class AuditLogResponse {

    @Schema(description = "Identificador único do registro de auditoria")
    private UUID id;

    @Schema(description = "Identificador do evento associado", example = "EVT-QLT-4091")
    private String eventId;

    @Schema(description = "Ação ou estágio auditado", example = "QUALITY_EVENT_REGISTERED")
    private String action;

    @Schema(description = "Timestamp imutável da auditoria")
    private LocalDateTime createdAt;

    public static AuditLogResponse fromDomain(AuditLog domain) {
        return AuditLogResponse.builder()
                .id(domain.getId())
                .eventId(domain.getEventId())
                .action(domain.getAction())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
