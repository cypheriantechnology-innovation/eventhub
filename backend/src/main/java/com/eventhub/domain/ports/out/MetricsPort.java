package com.eventhub.domain.ports.out;

import com.eventhub.domain.model.EventType;

import java.time.Duration;

public interface MetricsPort {

    void incrementProcessed(EventType eventType);

    void incrementDuplicate();

    void recordProcessingTime(EventType eventType, Duration duration);
}
