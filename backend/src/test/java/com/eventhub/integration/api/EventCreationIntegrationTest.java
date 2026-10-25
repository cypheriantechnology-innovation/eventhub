package com.eventhub.integration.api;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EventCreationIntegrationTest {

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
    @DisplayName("POST /api/events com payload de produção válido deve retornar 201 Created")
    void shouldReturn201ForValidProductionEvent() throws Exception {
        Event mockSaved = Event.create("EVT-1001", EventType.PRODUCTION, "Linha A iniciada", LocalDateTime.now());
        when(processEventUseCase.process(any(Event.class))).thenReturn(mockSaved);

        Map<String, Object> payload = Map.of(
                "eventId", "EVT-1001",
                "eventType", "PRODUCTION",
                "description", "Linha A iniciada",
                "timestamp", "2026-05-20T10:00:00"
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.eventType").value("PRODUCTION"))
                .andExpect(jsonPath("$.description").value("Linha A iniciada"));
    }

    @Test
    @DisplayName("POST /api/events com campos faltantes deve retornar 400 Bad Request")
    void shouldReturn400ForMissingFields() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "eventId", "",
                "description", ""
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/events com tipo de evento inválido deve retornar 400 Bad Request")
    void shouldReturn400ForInvalidEnumType() throws Exception {
        String invalidEnumJson = "{\"eventId\":\"EVT-BAD\",\"eventType\":\"INVALID\",\"description\":\"Sensor failure\"}";

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnumJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/events/stats deve retornar 200 OK com estatísticas agregadas")
    void shouldReturn200ForGetStats() throws Exception {
        when(findEventsUseCase.getStats()).thenReturn(new com.eventhub.domain.model.EventStats(100, 70, 30));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.production").value(70))
                .andExpect(jsonPath("$.quality").value(30));
    }
}
