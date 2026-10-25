package com.eventhub.application.usecase;

import com.eventhub.application.strategy.EventProcessor;
import com.eventhub.domain.exception.DuplicateEventException;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.ports.in.ProcessEventUseCase;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import com.eventhub.domain.ports.out.MetricsPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessEventService implements ProcessEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventService.class);

    private final List<EventProcessor> eventProcessors;
    private final EventRepositoryPort eventRepositoryPort;
    private final MetricsPort metricsPort;

    @Override
    @Transactional
    public Event process(Event event) {
        log.debug("Iniciando processamento do evento: eventId={}", event.getEventId());

        if (eventRepositoryPort.existsByEventId(event.getEventId())) {
            if (metricsPort != null) {
                metricsPort.incrementDuplicate();
            }
            throw new DuplicateEventException(event.getEventId());
        }

        EventProcessor processor = eventProcessors.stream()
                .filter(p -> p.supports(event.getEventType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de evento não suportado: " + event.getEventType()));

        Instant start = Instant.now();
        Event saved = processor.process(event);
        Duration duration = Duration.between(start, Instant.now());

        if (metricsPort != null) {
            metricsPort.incrementProcessed(event.getEventType());
            metricsPort.recordProcessingTime(event.getEventType(), duration);
        }

        log.info("Evento processado com sucesso: id={}, eventId={}, tipo={}",
                saved.getId(), saved.getEventId(), saved.getEventType());
        return saved;
    }
}
