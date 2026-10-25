package com.eventhub.infrastructure.adapters.out.metrics;

import com.eventhub.domain.model.EventType;
import com.eventhub.domain.ports.out.MetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private final MeterRegistry meterRegistry;
    private final Counter duplicateCounter;
    private final Counter productionCounter;
    private final Counter qualityCounter;

    public MicrometerMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.duplicateCounter = Counter.builder("events.duplicate")
                .description("Total de eventos duplicados rejeitados")
                .register(meterRegistry);
        this.productionCounter = Counter.builder("events.production")
                .description("Total de eventos de producao processados")
                .register(meterRegistry);
        this.qualityCounter = Counter.builder("events.quality")
                .description("Total de eventos de qualidade processados")
                .register(meterRegistry);
    }

    @Override
    public void incrementProcessed(EventType eventType) {
        Counter.builder("events.processed")
                .tag("type", eventType.name())
                .description("Total de eventos industriais processados com sucesso")
                .register(meterRegistry)
                .increment();

        if (EventType.PRODUCTION == eventType) {
            productionCounter.increment();
        } else if (EventType.QUALITY == eventType) {
            qualityCounter.increment();
        }
    }

    @Override
    public void incrementDuplicate() {
        duplicateCounter.increment();
    }

    @Override
    public void recordProcessingTime(EventType eventType, Duration duration) {
        Timer.builder("events.processing.time")
                .tag("type", eventType.name())
                .description("Tempo de processamento de eventos")
                .register(meterRegistry)
                .record(duration);
    }
}
