package com.eventhub.infrastructure.adapters.in.web;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.in.FindAuditLogsUseCase;
import com.eventhub.infrastructure.adapters.in.web.dto.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints para consulta de trilhas de auditoria regulatórias (audit_logs)")
public class AuditLogController {

    private final FindAuditLogsUseCase findAuditLogsUseCase;

    @GetMapping
    @Operation(summary = "Consulta paginada ou filtrada por eventId da trilha de auditoria")
    public ResponseEntity<?> getAuditLogs(
            @Parameter(description = "Identificador opcional do evento para filtro direto")
            @RequestParam(required = false) String eventId,
            @Parameter(description = "Número da página (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página (máximo 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        if (eventId != null && !eventId.trim().isEmpty()) {
            List<AuditLogResponse> list = findAuditLogsUseCase.findByEventId(eventId.trim()).stream()
                    .map(AuditLogResponse::fromDomain)
                    .toList();
            return ResponseEntity.ok(list);
        }

        int clampedSize = Math.min(Math.max(1, size), 100);
        int clampedPage = Math.max(0, page);

        PageResult<AuditLog> domainPage = findAuditLogsUseCase.findAll(clampedPage, clampedSize);
        List<AuditLogResponse> responseContent = domainPage.getContent().stream()
                .map(AuditLogResponse::fromDomain)
                .toList();

        PageResult<AuditLogResponse> pagedResponse = new PageResult<>(
                responseContent,
                domainPage.getPageNumber(),
                domainPage.getPageSize(),
                domainPage.getTotalElements()
        );

        return ResponseEntity.ok(pagedResponse);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Consulta de auditoria por identificador de evento")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByEventId(
            @PathVariable String eventId
    ) {
        List<AuditLogResponse> list = findAuditLogsUseCase.findByEventId(eventId).stream()
                .map(AuditLogResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(list);
    }
}
