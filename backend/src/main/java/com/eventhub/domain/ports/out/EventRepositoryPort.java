package com.eventhub.domain.ports.out;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.PageResult;

import java.util.Optional;

public interface EventRepositoryPort {

    Event save(Event event);

    Optional<Event> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    PageResult<Event> findAll(int page, int size, String sortDirection);
}
