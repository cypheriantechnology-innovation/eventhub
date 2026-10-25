package com.eventhub.domain.ports.in;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.PageResult;

public interface FindEventsUseCase {

    PageResult<Event> findAll(int page, int size, String sortDirection);

    Event findByEventId(String eventId);

    com.eventhub.domain.model.EventStats getStats();

    void invalidateStatsCache();
}
