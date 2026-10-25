package com.eventhub.infrastructure.adapters.in.web.dto;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
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
public class EventResponse {

    private UUID id;
    private String eventId;
    private EventType eventType;
    private String description;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;

    public static EventResponse fromDomain(Event event) {
        if (event == null) return null;
        return EventResponse.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .description(event.getDescription())
                .timestamp(event.getTimestamp())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
