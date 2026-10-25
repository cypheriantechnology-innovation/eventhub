package com.eventhub.unit.application;

import com.eventhub.application.usecase.FindEventsService;
import com.eventhub.domain.exception.EventNotFoundException;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.out.EventRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindEventsServiceTest {

    @Mock
    private EventRepositoryPort eventRepositoryPort;

    @InjectMocks
    private FindEventsService findEventsService;

    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.create("EVT-1001", EventType.PRODUCTION, "Início de linha A", LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve listar eventos paginados consultando a porta com ordenação especificada")
    void shouldFindAllPaginated() {
        PageResult<Event> mockPage = new PageResult<>(List.of(event), 0, 10, 1L);
        when(eventRepositoryPort.findAll(0, 10, "desc")).thenReturn(mockPage);

        PageResult<Event> result = findEventsService.findAll(0, 10, "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(eventRepositoryPort).findAll(0, 10, "desc");
    }

    @Test
    @DisplayName("Deve buscar evento existente por eventId")
    void shouldFindByEventId() {
        when(eventRepositoryPort.findByEventId("EVT-1001")).thenReturn(Optional.of(event));

        Event result = findEventsService.findByEventId("EVT-1001");

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo("EVT-1001");
        verify(eventRepositoryPort).findByEventId("EVT-1001");
    }

    @Test
    @DisplayName("Deve lançar EventNotFoundException quando o evento não existir")
    void shouldThrowWhenEventNotFound() {
        when(eventRepositoryPort.findByEventId("EVT-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> findEventsService.findByEventId("EVT-9999"))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("EVT-9999");
    }
}
