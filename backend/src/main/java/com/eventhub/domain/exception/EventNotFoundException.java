package com.eventhub.domain.exception;

public class EventNotFoundException extends RuntimeException {

    private final String eventId;

    public EventNotFoundException(String eventId) {
        super(String.format("Evento com identificador '%s' não foi encontrado.", eventId));
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
