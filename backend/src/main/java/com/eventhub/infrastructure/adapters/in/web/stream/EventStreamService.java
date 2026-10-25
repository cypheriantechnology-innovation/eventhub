package com.eventhub.infrastructure.adapters.in.web.stream;

import com.eventhub.infrastructure.adapters.in.web.dto.EventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class EventStreamService {

    // 30-minute timeout for industrial telemetry clients
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.add(emitter);
        log.info("[SSE] New client subscribed. Total active listeners: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("[SSE] Client disconnected (completed). Remaining: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("[SSE] Client timed out. Remaining: {}", emitters.size());
        });

        emitter.onError(e -> {
            emitters.remove(emitter);
            log.warn("[SSE] Client connection error: {}. Remaining: {}", e.getMessage(), emitters.size());
        });

        // Send initial handshake ping
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Connected to EventHub Telemetry Stream"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcast(EventResponse event) {
        if (emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("EVENT_INGESTED")
                        .data(event));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }
}
