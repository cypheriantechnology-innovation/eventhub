package com.eventhub.unit.adapters;

import com.eventhub.domain.model.EventType;
import com.eventhub.infrastructure.adapters.out.metrics.MicrometerMetricsAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerMetricsAdapterTest {

    private SimpleMeterRegistry meterRegistry;
    private MicrometerMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        adapter = new MicrometerMetricsAdapter(meterRegistry);
    }

    @Test
    @DisplayName("Deve incrementar contadores de processamento para produção e qualidade")
    void shouldIncrementProcessedCounters() {
        adapter.incrementProcessed(EventType.PRODUCTION);
        adapter.incrementProcessed(EventType.PRODUCTION);
        adapter.incrementProcessed(EventType.QUALITY);

        assertThat(meterRegistry.get("events.production").counter().count()).isEqualTo(2.0);
        assertThat(meterRegistry.get("events.quality").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Deve incrementar contador de eventos duplicados")
    void shouldIncrementDuplicateCounter() {
        adapter.incrementDuplicate();
        adapter.incrementDuplicate();

        assertThat(meterRegistry.get("events.duplicate").counter().count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Deve registrar tempo de processamento")
    void shouldRecordProcessingTime() {
        adapter.recordProcessingTime(EventType.PRODUCTION, Duration.ofMillis(150));

        assertThat(meterRegistry.get("events.processing.time")
                .tag("type", "PRODUCTION")
                .timer()
                .count()).isEqualTo(1);
    }
}
