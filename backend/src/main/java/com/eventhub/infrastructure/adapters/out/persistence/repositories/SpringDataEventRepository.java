package com.eventhub.infrastructure.adapters.out.persistence.repositories;

import com.eventhub.infrastructure.adapters.out.persistence.entities.EventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEventRepository extends JpaRepository<EventJpaEntity, UUID> {

    Optional<EventJpaEntity> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
