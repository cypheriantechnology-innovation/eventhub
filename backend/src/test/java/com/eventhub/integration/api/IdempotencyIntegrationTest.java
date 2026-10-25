package com.eventhub.integration.api;

import com.eventhub.domain.exception.DuplicateEventException;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.ports.in.FindEventsUseCase;
import com.eventhub.domain.ports.in.ProcessEventUseCase;
import com.eventhub.infrastructure.adapters.in.web.EventController;
import com.eventhub.infrastructure.adapters.in.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessEventUseCase processEventUseCase;

    @MockBean
    private FindEventsUseCase findEventsUseCase;

    @MockBean
    private com.eventhub.infrastructure.security.upload.FileUploadSecurityValidator fileUploadSecurityValidator;

    @MockBean
    private com.eventhub.infrastructure.adapters.in.web.stream.EventStreamService eventStreamService;

    @Test
    @DisplayName("Tentativa de submeter eventId duplicado deve retornar 409 Conflict")
    void shouldReturn409ForDuplicateEventId() throws Exception {
        when(processEventUseCase.process(any(Event.class)))
                .thenThrow(new DuplicateEventException("EVT-1001"));

        Map<String, Object> payload = Map.of(
                "eventId", "EVT-1001",
                "eventType", "PRODUCTION",
                "description", "Tentativa de reprocessamento"
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Evento com identificador 'EVT-1001' já foi processado anteriormente."));
    }
}
