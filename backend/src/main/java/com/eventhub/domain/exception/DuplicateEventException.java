package com.eventhub.domain.exception;

public class DuplicateEventException extends RuntimeException {

    private final String eventId;

    public DuplicateEventException(String eventId) {
        super(String.format("Evento com identificador '%s' já foi processado anteriormente.", eventId));
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
