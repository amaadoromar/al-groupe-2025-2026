package org.eSante.controllers;

import org.eSante.interfaces.RealtimeNotificationService;
import org.eSante.model.RealtimeNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/realtime")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RealtimeNotificationController {

    private final RealtimeNotificationService realtimeNotificationService;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody RealtimeNotification notification) {
        try {
            log.info("Sending realtime notification: {}", notification);

            realtimeNotificationService.sendNotification(notification);

            return ResponseEntity.ok("Notification sent successfully");
        } catch (Exception e) {
            log.error("Error while sending realtime notification", e);
            return ResponseEntity.internalServerError()
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        realtimeNotificationService.sendInfo(
                "Test Notification",
                "This is a test realtime notification."
        );
        return ResponseEntity.ok("Notification test sent");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Realtime Notification Service is up and running");
    }

}
