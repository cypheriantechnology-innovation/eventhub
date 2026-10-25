package com.eventhub.domain.ports.in;

import com.eventhub.domain.model.Event;

public interface ProcessEventUseCase {

    Event process(Event event);
}
