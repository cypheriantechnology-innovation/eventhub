package com.eventhub.infrastructure.adapters.in.web.dto;

import com.eventhub.domain.model.EventStats;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estatísticas agregadas de telemetria e eventos industriais")
public class EventStatsResponse {

    @Schema(description = "Total de eventos ingeridos com sucesso", example = "150")
    private long total;

    @Schema(description = "Total de eventos operacionais de produção", example = "105")
    private long production;

    @Schema(description = "Total de eventos de qualidade auditados", example = "45")
    private long quality;

    public static EventStatsResponse fromDomain(EventStats stats) {
        if (stats == null) return new EventStatsResponse(0, 0, 0);
        return EventStatsResponse.builder()
                .total(stats.total())
                .production(stats.production())
                .quality(stats.quality())
                .build();
    }
}
