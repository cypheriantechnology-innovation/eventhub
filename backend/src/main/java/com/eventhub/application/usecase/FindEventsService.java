package com.eventhub.application.usecase;

import com.eventhub.domain.exception.EventNotFoundException;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.in.FindEventsUseCase;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindEventsService implements FindEventsUseCase {

    private final EventRepositoryPort eventRepositoryPort;

    @Override
    public PageResult<Event> findAll(int page, int size, String sortDirection) {
        return eventRepositoryPort.findAll(page, size, sortDirection);
    }

    @Override
    public Event findByEventId(String eventId) {
        return eventRepositoryPort.findByEventId(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private record CachedStats(com.eventhub.domain.model.EventStats stats, long timestamp) {}
    private final java.util.concurrent.atomic.AtomicReference<CachedStats> statsCache = new java.util.concurrent.atomic.AtomicReference<>();
    private static final long STATS_TTL_MS = 10_000L; // 10 seconds defensive cache

    @Override
    public com.eventhub.domain.model.EventStats getStats() {
        long now = System.currentTimeMillis();
        CachedStats cached = statsCache.get();
        if (cached != null && (now - cached.timestamp()) < STATS_TTL_MS) {
            return cached.stats();
        }

        long total = eventRepositoryPort.countTotal();
        long production = eventRepositoryPort.countByEventType(com.eventhub.domain.model.EventType.PRODUCTION);
        long quality = eventRepositoryPort.countByEventType(com.eventhub.domain.model.EventType.QUALITY);
        com.eventhub.domain.model.EventStats freshStats = new com.eventhub.domain.model.EventStats(total, production, quality);

        statsCache.set(new CachedStats(freshStats, now));
        return freshStats;
    }
}
