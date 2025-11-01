package org.eSante.notification.api;

import jakarta.validation.Valid;

import org.eSante.notification.api.dto.NotificationRequest;
import org.eSante.notification.api.dto.NotificationResponse;

import org.eSante.notification.domain.NotificationStatus;
import org.eSante.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        var resp = notificationService.createAndSend(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@RequestParam String recipientId,
                                                           @RequestParam(required = false) NotificationStatus status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        var list = notificationService.listForRecipient(recipientId, status, page, size);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
    }
}

