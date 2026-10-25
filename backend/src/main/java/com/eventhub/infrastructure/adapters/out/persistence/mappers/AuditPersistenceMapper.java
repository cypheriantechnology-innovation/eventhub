package com.eventhub.infrastructure.adapters.out.persistence.mappers;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.infrastructure.adapters.out.persistence.entities.AuditLogJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditPersistenceMapper {

    public AuditLogJpaEntity toJpaEntity(AuditLog domain) {
        if (domain == null) return null;
        return AuditLogJpaEntity.builder()
                .id(domain.getId())
                .eventId(domain.getEventId())
                .action(domain.getAction())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public AuditLog toDomain(AuditLogJpaEntity entity) {
        if (entity == null) return null;
        return new AuditLog(
                entity.getId(),
                entity.getEventId(),
                entity.getAction(),
                entity.getCreatedAt()
        );
    }
}
