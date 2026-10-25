package com.eventhub.unit.domain;

import com.eventhub.domain.exception.DuplicateEventException;
import com.eventhub.domain.exception.EventNotFoundException;
import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventStats;
import com.eventhub.domain.model.EventType;
import com.eventhub.domain.model.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventDomainTest {

    @Test
    @DisplayName("Event.create deve instanciar entidade de domínio com UUID gerado e dados preservados")
    void shouldCreateEventSuccessfully() {
        LocalDateTime now = LocalDateTime.now();
        Event event = Event.create("EVT-100", EventType.PRODUCTION, "Sensor de pressão", now);

        assertThat(event.getId()).isNotNull();
        assertThat(event.getEventId()).isEqualTo("EVT-100");
        assertThat(event.getEventType()).isEqualTo(EventType.PRODUCTION);
        assertThat(event.getDescription()).isEqualTo("Sensor de pressão");
        assertThat(event.getTimestamp()).isEqualTo(now);
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Event construtor completo com UUID e createdAt nulos deve gerar fallbacks")
    void shouldHandleNullFallbacksInConstructor() {
        Event event = new Event(null, "EVT-NULL-TEST", EventType.QUALITY, "Teste null", null, null);

        assertThat(event.getId()).isNotNull();
        assertThat(event.getEventId()).isEqualTo("EVT-NULL-TEST");
        assertThat(event.getEventType()).isEqualTo(EventType.QUALITY);
        assertThat(event.getDescription()).isEqualTo("Teste null");
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Event construtor deve rejeitar campos obrigatórios nulos com NullPointerException")
    void shouldRejectNullMandatoryFieldsInEvent() {
        assertThatThrownBy(() -> new Event(UUID.randomUUID(), null, EventType.PRODUCTION, "Desc", null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId must not be null");

        assertThatThrownBy(() -> new Event(UUID.randomUUID(), "EVT-1", null, "Desc", null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventType must not be null");

        assertThatThrownBy(() -> new Event(UUID.randomUUID(), "EVT-1", EventType.PRODUCTION, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description must not be null");
    }

    @Test
    @DisplayName("Event deve respeitar contrato de equals, hashCode e toString")
    void shouldVerifyEventEqualsHashCodeAndToString() {
        UUID id1 = UUID.randomUUID();
        Event evt1 = new Event(id1, "EVT-SAME", EventType.PRODUCTION, "D1", null, null);
        Event evt2 = new Event(UUID.randomUUID(), "EVT-SAME", EventType.QUALITY, "D2", null, null);
        Event evt3 = new Event(UUID.randomUUID(), "EVT-OTHER", EventType.PRODUCTION, "D3", null, null);

        assertThat(evt1).isEqualTo(evt1);
        assertThat(evt1).isEqualTo(evt2);
        assertThat(evt1).isNotEqualTo(evt3);
        assertThat(evt1).isNotEqualTo(null);
        assertThat(evt1).isNotEqualTo("some string");

        assertThat(evt1.hashCode()).isEqualTo(evt2.hashCode());
        assertThat(evt1.toString()).contains("EVT-SAME").contains("PRODUCTION");
    }

    @Test
    @DisplayName("AuditLog.create e construtor devem inicializar propriedades e respeitar validações")
    void shouldCreateAndValidateAuditLog() {
        AuditLog audit = AuditLog.create("EVT-AUDIT-1", "PROCESSED_QUALITY");

        assertThat(audit.getId()).isNotNull();
        assertThat(audit.getEventId()).isEqualTo("EVT-AUDIT-1");
        assertThat(audit.getAction()).isEqualTo("PROCESSED_QUALITY");
        assertThat(audit.getCreatedAt()).isNotNull();

        // Null fallbacks
        AuditLog auditFallback = new AuditLog(null, "EVT-2", "ACTION-2", null);
        assertThat(auditFallback.getId()).isNotNull();
        assertThat(auditFallback.getCreatedAt()).isNotNull();

        // Null checks
        assertThatThrownBy(() -> new AuditLog(UUID.randomUUID(), null, "ACT", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId must not be null");

        assertThatThrownBy(() -> new AuditLog(UUID.randomUUID(), "EVT-3", null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("action must not be null");
    }

    @Test
    @DisplayName("AuditLog deve respeitar contrato de equals, hashCode e toString")
    void shouldVerifyAuditLogEqualsHashCodeAndToString() {
        UUID id = UUID.randomUUID();
        AuditLog log1 = new AuditLog(id, "EVT-A", "ACT-A", null);
        AuditLog log2 = new AuditLog(id, "EVT-B", "ACT-B", null);
        AuditLog log3 = new AuditLog(UUID.randomUUID(), "EVT-A", "ACT-A", null);

        assertThat(log1).isEqualTo(log1);
        assertThat(log1).isEqualTo(log2);
        assertThat(log1).isNotEqualTo(log3);
        assertThat(log1).isNotEqualTo(null);
        assertThat(log1).isNotEqualTo(new Object());

        assertThat(log1.hashCode()).isEqualTo(log2.hashCode());
        assertThat(log1.toString()).contains(id.toString()).contains("EVT-A");
    }

    @Test
    @DisplayName("EventStats deve armazenar e retornar contagens totais e segmentadas corretamente")
    void shouldCreateEventStatsRecord() {
        EventStats stats = new EventStats(100L, 70L, 30L);

        assertThat(stats.total()).isEqualTo(100L);
        assertThat(stats.production()).isEqualTo(70L);
        assertThat(stats.quality()).isEqualTo(30L);
    }

    @Test
    @DisplayName("PageResult deve calcular total de páginas e propriedades de paginação")
    void shouldCalculatePaginationCorrectly() {
        List<String> items = List.of("A", "B", "C");
        PageResult<String> page = new PageResult<>(items, 0, 10, 25L);

        assertThat(page.getContent()).containsExactly("A", "B", "C");
        assertThat(page.getPageNumber()).isZero();
        assertThat(page.getPageSize()).isEqualTo(10);
        assertThat(page.getTotalElements()).isEqualTo(25L);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.isLast()).isFalse();

        // Non-positive pageSize fallback
        PageResult<String> fallbackPage = new PageResult<>(null, 1, 0, 10L);
        assertThat(fallbackPage.getContent()).isEmpty();
        assertThat(fallbackPage.getPageSize()).isEqualTo(20);
        assertThat(fallbackPage.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("PageResult com total zero deve resultar em 0 páginas e isLast true")
    void shouldHandleEmptyPageResult() {
        PageResult<String> emptyPage = new PageResult<>(List.of(), 0, 10, 0L);

        assertThat(emptyPage.getTotalPages()).isEqualTo(0);
        assertThat(emptyPage.isLast()).isTrue();
    }

    @Test
    @DisplayName("DuplicateEventException deve conter identificador e mensagem explicativa")
    void shouldThrowDuplicateEventException() {
        DuplicateEventException ex = new DuplicateEventException("EVT-DUPLICATE");

        assertThat(ex.getMessage()).contains("EVT-DUPLICATE");
        assertThat(ex.getMessage()).contains("já foi processado anteriormente");
    }

    @Test
    @DisplayName("EventNotFoundException deve conter identificador e mensagem explicativa")
    void shouldThrowEventNotFoundException() {
        EventNotFoundException ex = new EventNotFoundException("EVT-404");

        assertThat(ex.getMessage()).contains("EVT-404");
        assertThat(ex.getMessage()).contains("não foi encontrado");
    }
}
