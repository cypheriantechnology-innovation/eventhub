package com.eventhub.application.strategy;

import com.eventhub.domain.model.Event;
import com.eventhub.domain.model.EventType;

public interface EventProcessor {

    boolean supports(EventType eventType);

    Event process(Event event);
}
