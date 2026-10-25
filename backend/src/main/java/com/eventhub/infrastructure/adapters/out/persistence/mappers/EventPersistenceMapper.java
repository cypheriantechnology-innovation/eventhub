package com.eventhub.infrastructure.adapters.out.persistence.mappers;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.infrastructure.adapters.out.persistence.entities.EventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class EventPersistenceMapper {

    public EventJpaEntity toJpaEntity(Event domain) {
        if (domain == null) return null;
        return EventJpaEntity.builder()
                .id(domain.getId())
                .eventId(domain.getEventId())
                .eventType(domain.getEventType().name())
                .description(domain.getDescription())
                .timestamp(domain.getTimestamp())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public Event toDomain(EventJpaEntity entity) {
        if (entity == null) return null;
        return new Event(
                entity.getId(),
                entity.getEventId(),
                EventType.valueOf(entity.getEventType()),
                entity.getDescription(),
                entity.getTimestamp(),
                entity.getCreatedAt()
        );
    }
}
