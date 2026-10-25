package com.eventhub.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class AuditLog {

    private final UUID id;
    private final String eventId;
    private final String action;
    private final LocalDateTime createdAt;

    public AuditLog(UUID id, String eventId, String action, LocalDateTime createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static AuditLog create(String eventId, String action) {
        return new AuditLog(UUID.randomUUID(), eventId, action, LocalDateTime.now());
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", eventId='" + eventId + '\'' +
                ", action='" + action + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
