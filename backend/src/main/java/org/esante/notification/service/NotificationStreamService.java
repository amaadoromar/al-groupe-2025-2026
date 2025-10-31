package org.esante.notification.service;

import org.esante.notification.api.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationStreamService {
    private static final Logger log = LoggerFactory.getLogger(NotificationStreamService.class);

    private final Map<String, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String recipientId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        subscribers.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(recipientId, emitter));
        emitter.onTimeout(() -> remove(recipientId, emitter));
        emitter.onError((ex) -> remove(recipientId, emitter));
        try {
            emitter.send(SseEmitter.event().name("INIT").data("subscribed").id("init").reconnectTime(3000));
        } catch (IOException ignored) {}
        return emitter;
    }

    public void broadcast(String recipientId, NotificationResponse notification) {
        List<SseEmitter> list = subscribers.get(recipientId);
        if (list == null || list.isEmpty()) return;
        List<SseEmitter> toRemove = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification, MediaType.APPLICATION_JSON)
                );
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        toRemove.forEach(em -> remove(recipientId, em));
    }

    private void remove(String recipientId, SseEmitter emitter) {
        List<SseEmitter> list = subscribers.get(recipientId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) subscribers.remove(recipientId);
        }
    }
}

