package com.eventhub.infrastructure.adapters.in.web;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.in.FindEventsUseCase;
import com.eventhub.domain.ports.in.ProcessEventUseCase;
import com.eventhub.infrastructure.adapters.in.web.dto.CreateEventRequest;
import com.eventhub.infrastructure.adapters.in.web.dto.EventResponse;
import com.eventhub.infrastructure.adapters.in.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Endpoints para recepção, processamento e consulta de eventos industriais")
public class EventController {

    private final ProcessEventUseCase processEventUseCase;
    private final FindEventsUseCase findEventsUseCase;

    @PostMapping
    @Operation(summary = "Ingestão e processamento de evento industrial")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        Event domainEvent = Event.create(
                request.getEventId(),
                request.getEventType(),
                request.getDescription(),
                request.getTimestamp()
        );

        Event processed = processEventUseCase.process(domainEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.fromDomain(processed));
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
}
