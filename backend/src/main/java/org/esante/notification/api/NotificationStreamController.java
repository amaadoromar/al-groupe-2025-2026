package org.esante.notification.api;

import org.esante.notification.service.NotificationStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class NotificationStreamController {
    private final NotificationStreamService streamService;

    public NotificationStreamController(NotificationStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping(path = "/api/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String recipientId) {
        return streamService.subscribe(recipientId);
    }
}

