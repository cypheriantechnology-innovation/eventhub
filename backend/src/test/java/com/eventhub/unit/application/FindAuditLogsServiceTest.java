package com.eventhub.unit.application;

import com.eventhub.application.usecase.FindAuditLogsService;
import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.out.AuditRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAuditLogsServiceTest {

    @Mock
    private AuditRepositoryPort auditRepositoryPort;

    private FindAuditLogsService findAuditLogsService;

    @BeforeEach
    void setUp() {
        findAuditLogsService = new FindAuditLogsService(auditRepositoryPort);
    }

    @Test
    @DisplayName("findByEventId deve delegar para porta de auditoria quando eventId for válido")
    void shouldFindAuditLogsByEventId() {
        AuditLog log = AuditLog.create("EVT-QLT-1", "QUALITY_EVENT_REGISTERED");
        when(auditRepositoryPort.findByEventId("EVT-QLT-1")).thenReturn(List.of(log));

        List<AuditLog> results = findAuditLogsService.findByEventId("EVT-QLT-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEventId()).isEqualTo("EVT-QLT-1");
        verify(auditRepositoryPort).findByEventId("EVT-QLT-1");
    }

    @Test
    @DisplayName("findByEventId com string nula ou vazia deve retornar lista vazia sem chamar banco")
    void shouldReturnEmptyWhenEventIdIsBlank() {
        assertThat(findAuditLogsService.findByEventId(null)).isEmpty();
        assertThat(findAuditLogsService.findByEventId("   ")).isEmpty();
    }

    @Test
    @DisplayName("findAll deve delegar para porta de auditoria com paginação")
    void shouldFindAllPaginated() {
        PageResult<AuditLog> pageResult = new PageResult<>(List.of(), 0, 10, 0L);
        when(auditRepositoryPort.findAll(0, 10)).thenReturn(pageResult);

        PageResult<AuditLog> result = findAuditLogsService.findAll(0, 10);

        assertThat(result).isNotNull();
        verify(auditRepositoryPort).findAll(0, 10);
    }
}
