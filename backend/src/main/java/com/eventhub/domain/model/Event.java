package com.eventhub.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Event {

    private final UUID id;
    private final String eventId;
    private final EventType eventType;
    private final String description;
    private final LocalDateTime timestamp;
    private final LocalDateTime createdAt;

    public Event(UUID id, String eventId, EventType eventType, String description, LocalDateTime timestamp, LocalDateTime createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Event create(String eventId, EventType eventType, String description, LocalDateTime timestamp) {
        return new Event(UUID.randomUUID(), eventId, eventType, description, timestamp, LocalDateTime.now());
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(eventId, event.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", eventId='" + eventId + '\'' +
                ", eventType=" + eventType +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                ", createdAt=" + createdAt +
                '}';
    }
}
