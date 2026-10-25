package com.eventhub.infrastructure.adapters.out.persistence.repositories;

import com.eventhub.infrastructure.adapters.out.persistence.entities.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAuditRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    List<AuditLogJpaEntity> findByEventIdOrderByCreatedAtDesc(String eventId);

    Page<AuditLogJpaEntity> findAll(Pageable pageable);
}
