package com.eventhub.application.strategy;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.ports.out.AuditRepositoryPort;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QualityEventProcessor implements EventProcessor {

    private static final Logger log = LoggerFactory.getLogger(QualityEventProcessor.class);

    private final EventRepositoryPort eventRepositoryPort;
    private final AuditRepositoryPort auditRepositoryPort;

    @Override
    public boolean supports(EventType eventType) {
        return EventType.QUALITY == eventType;
    }

    @Override
    public Event process(Event event) {
        log.info("Processando evento de QUALIDADE: eventId={}", event.getEventId());

        Event savedEvent = eventRepositoryPort.save(event);

        AuditLog auditLog = AuditLog.create(event.getEventId(), "QUALITY_EVENT_REGISTERED");
        auditRepositoryPort.save(auditLog);

        log.info("Registro de auditoria gerado com sucesso para evento de qualidade: eventId={}", event.getEventId());
        return savedEvent;
    }
}
