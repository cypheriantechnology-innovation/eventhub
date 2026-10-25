package com.eventhub.unit.application;

import com.eventhub.application.strategy.QualityEventProcessor;
import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.ports.out.AuditRepositoryPort;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualityEventProcessorTest {

    @Mock
    private EventRepositoryPort eventRepositoryPort;

    @Mock
    private AuditRepositoryPort auditRepositoryPort;

    @InjectMocks
    private QualityEventProcessor processor;

    private Event qualityEvent;

    @BeforeEach
    void setUp() {
        qualityEvent = Event.create("EVT-2001", EventType.QUALITY, "Desvio dimensional", LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve suportar exclusivamente eventos do tipo QUALITY")
    void shouldSupportOnlyQualityType() {
        assertThat(processor.supports(EventType.QUALITY)).isTrue();
        assertThat(processor.supports(EventType.PRODUCTION)).isFalse();
    }

    @Test
    @DisplayName("Deve persistir o evento de qualidade e gerar registro na trilha de auditoria")
    void shouldPersistEventAndCreateAuditLog() {
        when(eventRepositoryPort.save(any(Event.class))).thenReturn(qualityEvent);

        Event result = processor.process(qualityEvent);

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo("EVT-2001");
        verify(eventRepositoryPort).save(qualityEvent);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepositoryPort).save(auditCaptor.capture());

        AuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit).isNotNull();
        assertThat(savedAudit.getEventId()).isEqualTo("EVT-2001");
        assertThat(savedAudit.getAction()).isEqualTo("QUALITY_EVENT_REGISTERED");
    }
}
