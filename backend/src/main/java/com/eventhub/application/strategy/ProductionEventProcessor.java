package com.eventhub.application.strategy;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductionEventProcessor implements EventProcessor {

    private static final Logger log = LoggerFactory.getLogger(ProductionEventProcessor.class);
    private final EventRepositoryPort eventRepositoryPort;

    @Override
    public boolean supports(EventType eventType) {
        return EventType.PRODUCTION == eventType;
    }

    @Override
    public Event process(Event event) {
        log.info("Processando evento de PRODUÇÃO: eventId={}", event.getEventId());
        return eventRepositoryPort.save(event);
    }
}
