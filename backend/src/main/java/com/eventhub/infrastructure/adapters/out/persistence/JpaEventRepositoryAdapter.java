package com.eventhub.infrastructure.adapters.out.persistence;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import com.eventhub.infrastructure.adapters.out.persistence.entities.EventJpaEntity;
import com.eventhub.infrastructure.adapters.out.persistence.mappers.EventPersistenceMapper;
import com.eventhub.infrastructure.adapters.out.persistence.repositories.SpringDataEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaEventRepositoryAdapter implements EventRepositoryPort {

    private final SpringDataEventRepository springDataEventRepository;
    private final EventPersistenceMapper eventPersistenceMapper;

    @Override
    public Event save(Event event) {
        EventJpaEntity entity = eventPersistenceMapper.toJpaEntity(event);
        EventJpaEntity saved = springDataEventRepository.save(entity);
        return eventPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Event> findByEventId(String eventId) {
        return springDataEventRepository.findByEventId(eventId)
                .map(eventPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return springDataEventRepository.existsByEventId(eventId);
    }

    @Override
    public PageResult<Event> findAll(int page, int size, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, "timestamp"));
        Page<EventJpaEntity> pageResult = springDataEventRepository.findAll(pageRequest);

        List<Event> events = pageResult.getContent().stream()
                .map(eventPersistenceMapper::toDomain)
                .toList();

        return new PageResult<>(events, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
}
