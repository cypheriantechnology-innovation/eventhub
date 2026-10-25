package com.eventhub.unit.application;

import com.eventhub.application.strategy.ProductionEventProcessor;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionEventProcessorTest {

    @Mock
    private EventRepositoryPort eventRepositoryPort;

    @InjectMocks
    private ProductionEventProcessor processor;

    private Event productionEvent;

    @BeforeEach
    void setUp() {
        productionEvent = Event.create("EVT-1001", EventType.PRODUCTION, "Início de linha A", LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve suportar exclusivamente eventos do tipo PRODUCTION")
    void shouldSupportOnlyProductionType() {
        assertThat(processor.supports(EventType.PRODUCTION)).isTrue();
        assertThat(processor.supports(EventType.QUALITY)).isFalse();
    }

    @Test
    @DisplayName("Deve persistir o evento de produção sem gerar registros de auditoria")
    void shouldProcessAndPersistProductionEvent() {
        when(eventRepositoryPort.save(any(Event.class))).thenReturn(productionEvent);

        Event result = processor.process(productionEvent);

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo("EVT-1001");
        assertThat(result.getEventType()).isEqualTo(EventType.PRODUCTION);
        verify(eventRepositoryPort).save(productionEvent);
    }
}
