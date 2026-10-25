package com.eventhub.infrastructure.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Relatório de processamento de ingestão em lote de eventos")
public class BatchUploadResponse {

    @Schema(description = "Status do processamento", example = "ACCEPTED")
    @Builder.Default
    private String status = "ACCEPTED";

    @Schema(description = "Nome do arquivo sanitizado")
    private String filename;

    @Schema(description = "Total de registros encontrados no arquivo")
    private int totalRecords;

    @Schema(description = "Total de eventos ingeridos com sucesso")
    private int ingested;

    @Schema(description = "Total de eventos duplicados ignorados com idempotência")
    private int duplicates;

    @Schema(description = "Total de registros com erro de formatação")
    private int errors;

    @Schema(description = "Lista detalhada de mensagens explicativas de erros ou duplicatas")
    private List<String> details;
}
