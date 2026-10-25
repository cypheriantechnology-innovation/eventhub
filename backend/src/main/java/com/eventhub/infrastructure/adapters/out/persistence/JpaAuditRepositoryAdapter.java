package com.eventhub.infrastructure.adapters.out.persistence;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.ports.out.AuditRepositoryPort;
import com.eventhub.infrastructure.adapters.out.persistence.entities.AuditLogJpaEntity;
import com.eventhub.infrastructure.adapters.out.persistence.mappers.AuditPersistenceMapper;
import com.eventhub.infrastructure.adapters.out.persistence.repositories.SpringDataAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaAuditRepositoryAdapter implements AuditRepositoryPort {

    private final SpringDataAuditRepository springDataAuditRepository;
    private final AuditPersistenceMapper auditPersistenceMapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = auditPersistenceMapper.toJpaEntity(auditLog);
        AuditLogJpaEntity saved = springDataAuditRepository.save(entity);
        return auditPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<AuditLog> findByEventId(String eventId) {
        return springDataAuditRepository.findByEventId(eventId).stream()
                .map(auditPersistenceMapper::toDomain)
                .toList();
    }
}
