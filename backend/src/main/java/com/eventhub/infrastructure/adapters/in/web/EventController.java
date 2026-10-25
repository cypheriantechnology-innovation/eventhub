package com.eventhub.infrastructure.adapters.in.web;

import com.eventhub.domain.exception.DuplicateEventException;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.in.FindEventsUseCase;
import com.eventhub.domain.ports.in.ProcessEventUseCase;
import com.eventhub.infrastructure.adapters.in.web.dto.BatchUploadResponse;
import com.eventhub.infrastructure.adapters.in.web.dto.CreateEventRequest;
import com.eventhub.infrastructure.adapters.in.web.dto.EventResponse;
import com.eventhub.infrastructure.adapters.in.web.dto.EventStatsResponse;
import com.eventhub.infrastructure.adapters.in.web.dto.PageResponse;
import com.eventhub.infrastructure.adapters.in.web.stream.EventStreamService;
import com.eventhub.infrastructure.security.upload.FileUploadSecurityValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Events", description = "Endpoints para recepção, processamento, streaming e consulta de eventos industriais")
public class EventController {

    private final ProcessEventUseCase processEventUseCase;
    private final FindEventsUseCase findEventsUseCase;
    private final FileUploadSecurityValidator fileUploadSecurityValidator;
    private final EventStreamService eventStreamService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Ingestão e processamento de evento industrial")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        String sanitizedEventId = request.getEventId() != null ? request.getEventId().trim() : null;
        String sanitizedDescription = request.getDescription() != null ? request.getDescription().trim() : null;

        Event domainEvent = Event.create(
                sanitizedEventId,
                request.getEventType(),
                sanitizedDescription,
                request.getTimestamp()
        );

        Event processed = processEventUseCase.process(domainEvent);
        EventResponse response = EventResponse.fromDomain(processed);

        // Broadcast to real-time subscribers via SSE
        eventStreamService.broadcast(response);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas consolidadas de eventos industriais (total, produção, qualidade)")
    public ResponseEntity<EventStatsResponse> getStats() {
        com.eventhub.domain.model.EventStats stats = findEventsUseCase.getStats();
        return ResponseEntity.ok(EventStatsResponse.fromDomain(stats));
    }

    @GetMapping
    @Operation(summary = "Listagem paginada de eventos registrados ordenados decrescente por data/hora")
    public ResponseEntity<PageResponse<EventResponse>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        PageResult<Event> pageResult = findEventsUseCase.findAll(page, size, sort);
        PageResponse<EventResponse> response = PageResponse.fromPageResult(pageResult, EventResponse::fromDomain);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Consulta detalhada de evento por identificador único")
    public ResponseEntity<EventResponse> getEventById(@PathVariable String eventId) {
        Event event = findEventsUseCase.findByEventId(eventId);
        return ResponseEntity.ok(EventResponse.fromDomain(event));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Conexão Server-Sent Events (SSE) para recepção de telemetria em tempo real")
    public SseEmitter streamEvents() {
        return eventStreamService.subscribe();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload seguro e ingestão em lote de eventos industriais (CSV ou JSON)")
    public ResponseEntity<?> uploadEventBatch(@RequestParam("file") MultipartFile file) {
        var validation = fileUploadSecurityValidator.validate(file);
        if (!validation.isValid()) {
            String incidentId = java.util.UUID.randomUUID().toString();
            return ResponseEntity.badRequest().body(com.eventhub.infrastructure.adapters.in.web.dto.ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid Upload",
                    validation.errorMessage(),
                    incidentId
            ));
        }

        String filename = validation.sanitizedFilename();
        int totalRecords = 0;
        int ingested = 0;
        int duplicates = 0;
        int errors = 0;
        List<String> details = new ArrayList<>();

        try {
            if (filename.toLowerCase().endsWith(".json")) {
                List<CreateEventRequest> requests = objectMapper.readValue(
                        file.getInputStream(),
                        new TypeReference<List<CreateEventRequest>>() {}
                );
                totalRecords = requests.size();
                for (CreateEventRequest req : requests) {
                    try {
                        Event domainEvent = Event.create(
                                req.getEventId() != null ? req.getEventId().trim() : null,
                                req.getEventType(),
                                req.getDescription() != null ? req.getDescription().trim() : null,
                                req.getTimestamp()
                        );
                        Event processed = processEventUseCase.process(domainEvent);
                        eventStreamService.broadcast(EventResponse.fromDomain(processed));
                        ingested++;
                    } catch (DuplicateEventException e) {
                        duplicates++;
                        details.add("Duplicata ignorada: " + req.getEventId());
                    } catch (Exception e) {
                        errors++;
                        details.add("Erro no registro " + req.getEventId() + ": " + e.getMessage());
                    }
                }
            } else {
                // CSV Parsing
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean isFirstLine = true;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        if (isFirstLine) {
                            isFirstLine = false;
                            // Check if header line
                            if (line.toLowerCase().contains("eventid") || line.toLowerCase().contains("event_id")) {
                                continue;
                            }
                        }
                        totalRecords++;
                        String[] parts = line.split("[,;]");
                        if (parts.length < 3) {
                            errors++;
                            details.add("Linha inválida (mínimo 3 colunas: id, tipo, descrição): " + line);
                            continue;
                        }

                        String rawId = parts[0].trim().replaceAll("^\"|\"$", "");
                        String rawType = parts[1].trim().replaceAll("^\"|\"$", "").toUpperCase();
                        String rawDesc = parts[2].trim().replaceAll("^\"|\"$", "");
                        LocalDateTime timestamp = LocalDateTime.now();
                        if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                            String rawTs = parts[3].trim().replaceAll("^\"|\"$", "");
                            try {
                                if (rawTs.endsWith("Z")) {
                                    timestamp = java.time.Instant.parse(rawTs).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                                } else {
                                    timestamp = LocalDateTime.parse(rawTs, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                                }
                            } catch (Exception ex) {
                                timestamp = LocalDateTime.now();
                            }
                        }

                        try {
                            EventType eventType = EventType.valueOf(rawType);
                            Event domainEvent = Event.create(rawId, eventType, rawDesc, timestamp);
                            Event processed = processEventUseCase.process(domainEvent);
                            eventStreamService.broadcast(EventResponse.fromDomain(processed));
                            ingested++;
                        } catch (IllegalArgumentException e) {
                            errors++;
                            details.add("Tipo de evento inválido '" + rawType + "' no registro: " + rawId);
                        } catch (DuplicateEventException e) {
                            duplicates++;
                            details.add("Duplicata ignorada: " + rawId);
                        } catch (Exception e) {
                            errors++;
                            details.add("Erro no evento " + rawId + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[BATCH-ERROR] Failed to process batch file {}: {}", filename, e.getMessage());
            return ResponseEntity.badRequest().body(com.eventhub.infrastructure.adapters.in.web.dto.ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    "Batch Processing Error",
                    "Falha ao ler dados do arquivo: " + e.getMessage()
            ));
        }

        BatchUploadResponse batchResponse = BatchUploadResponse.builder()
                .filename(filename)
                .totalRecords(totalRecords)
                .ingested(ingested)
                .duplicates(duplicates)
                .errors(errors)
                .details(details.size() > 20 ? details.subList(0, 20) : details)
                .build();

        return ResponseEntity.ok(batchResponse);
    }
}
