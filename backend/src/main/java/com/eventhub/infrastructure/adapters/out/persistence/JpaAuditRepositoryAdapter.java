package com.eventhub.infrastructure.adapters.out.persistence;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.out.AuditRepositoryPort;
import com.eventhub.infrastructure.adapters.out.persistence.entities.AuditLogJpaEntity;
import com.eventhub.infrastructure.adapters.out.persistence.mappers.AuditPersistenceMapper;
import com.eventhub.infrastructure.adapters.out.persistence.repositories.SpringDataAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        return springDataAuditRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(auditPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<AuditLog> findAll(int page, int size) {
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = (size <= 0 || size > 100) ? 20 : size;

        PageRequest pageRequest = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogJpaEntity> jpaPage = springDataAuditRepository.findAll(pageRequest);

        List<AuditLog> domainList = jpaPage.getContent().stream()
                .map(auditPersistenceMapper::toDomain)
                .toList();

        return new PageResult<>(domainList, jpaPage.getNumber(), jpaPage.getSize(), jpaPage.getTotalElements());
    }
}
