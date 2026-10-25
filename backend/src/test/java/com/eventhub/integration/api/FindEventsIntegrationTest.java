package com.eventhub.integration.api;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.in.FindEventsUseCase;
import com.eventhub.domain.ports.in.ProcessEventUseCase;
import com.eventhub.infrastructure.adapters.in.web.EventController;
import com.eventhub.infrastructure.adapters.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FindEventsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessEventUseCase processEventUseCase;

    @MockBean
    private FindEventsUseCase findEventsUseCase;

    @MockBean
    private com.eventhub.infrastructure.security.upload.FileUploadSecurityValidator fileUploadSecurityValidator;

    @MockBean
    private com.eventhub.infrastructure.adapters.in.web.stream.EventStreamService eventStreamService;

    @Test
    @DisplayName("GET /api/events deve retornar lista paginada com sucesso")
    void shouldReturnPaginatedEvents() throws Exception {
        Event event1 = Event.create("EVT-2001", EventType.QUALITY, "Desvio dimensional", LocalDateTime.now());
        Event event2 = Event.create("EVT-1001", EventType.PRODUCTION, "Início de linha A", LocalDateTime.now().minusHours(1));

        PageResult<Event> mockPage = new PageResult<>(List.of(event1, event2), 0, 10, 2L);
        when(findEventsUseCase.findAll(0, 10, "desc")).thenReturn(mockPage);

        mockMvc.perform(get("/api/events?page=0&size=10&sort=desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].eventId").value("EVT-2001"))
                .andExpect(jsonPath("$.content[1].eventId").value("EVT-1001"));
    }

    @Test
    @DisplayName("GET /api/events/{eventId} deve retornar o evento solicitado")
    void shouldReturnEventByEventId() throws Exception {
        Event event = Event.create("EVT-1001", EventType.PRODUCTION, "Início de linha A", LocalDateTime.now());
        when(findEventsUseCase.findByEventId("EVT-1001")).thenReturn(event);

        mockMvc.perform(get("/api/events/EVT-1001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.eventType").value("PRODUCTION"));
    }
}
