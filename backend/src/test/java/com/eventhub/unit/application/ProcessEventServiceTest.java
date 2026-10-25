package com.eventhub.unit.application;

import com.eventhub.application.strategy.EventProcessor;
import com.eventhub.application.usecase.ProcessEventService;
import com.eventhub.domain.exception.DuplicateEventException;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import com.eventhub.domain.ports.out.MetricsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessEventServiceTest {

    @Mock
    private EventRepositoryPort eventRepositoryPort;

    @Mock
    private MetricsPort metricsPort;

    @Mock
    private EventProcessor productionProcessor;

    private ProcessEventService processEventService;

    private Event productionEvent;

    @BeforeEach
    void setUp() {
        processEventService = new ProcessEventService(
                List.of(productionProcessor),
                eventRepositoryPort,
                metricsPort
        );

        productionEvent = Event.create("EVT-1001", EventType.PRODUCTION, "Início de linha A", LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve processar evento inédito com sucesso roteando para a estratégia correta")
    void shouldProcessNewEventSuccessfully() {
        when(eventRepositoryPort.existsByEventId("EVT-1001")).thenReturn(false);
        when(productionProcessor.supports(EventType.PRODUCTION)).thenReturn(true);
        when(productionProcessor.process(any(Event.class))).thenReturn(productionEvent);

        Event result = processEventService.process(productionEvent);

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo("EVT-1001");
        verify(eventRepositoryPort).existsByEventId("EVT-1001");
        verify(productionProcessor).process(productionEvent);
        verify(metricsPort).incrementProcessed(EventType.PRODUCTION);
        verify(metricsPort).recordProcessingTime(eq(EventType.PRODUCTION), any(Duration.class));
    }

    @Test
    @DisplayName("Deve rejeitar evento duplicado com DuplicateEventException e incrementar métrica")
    void shouldRejectDuplicateEvent() {
        when(eventRepositoryPort.existsByEventId("EVT-1001")).thenReturn(true);

        assertThatThrownBy(() -> processEventService.process(productionEvent))
                .isInstanceOf(DuplicateEventException.class)
                .hasMessageContaining("EVT-1001");

        verify(metricsPort).incrementDuplicate();
        verifyNoInteractions(productionProcessor);
    }
}
