package fr.polytech.notification.service.controllers;

import fr.polytech.notification.service.interfaces.EmailService;
import fr.polytech.notification.service.interfaces.RealtimeNotificationService;
import fr.polytech.notification.service.model.EmailNotificationRequest;
import fr.polytech.notification.service.model.NotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/email")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmailController {

    private final EmailService emailService;
    private final RealtimeNotificationService realtimeNotificationService;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendEmail(
            @Valid @RequestBody EmailNotificationRequest request
            ) {
        try {
            log.info("Received email notification request to: {}", request.getTo());

            emailService.sendSimpleMail(request);

            realtimeNotificationService.sendSuccess(
                    "Mail Sent",
                    "Mail successfully sent to " + request.getTo()
            );

            return ResponseEntity.ok(
                    NotificationResponse.success("Mail sent successfully")
            );
        } catch (Exception e) {
            log.error("Error while sending email", e);

            realtimeNotificationService.sendError(
                    "Send Mail Failed",
                    "Unable to send mail: " + e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NotificationResponse.error("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/send-html")
    public ResponseEntity<NotificationResponse> sendHtmlEmail(
            @Valid @RequestBody EmailNotificationRequest request
    ) {
        try {
            log.info("Received HTML email notification request to: {}", request.getTo());

            emailService.sendHtmlMail(request);

            realtimeNotificationService.sendSuccess(
                    "HTML Mail Sent",
                    "HTML Mail successfully sent to " + request.getTo()
            );

            return ResponseEntity.ok(
                    NotificationResponse.success("HTML Mail sent successfully")
            );
        } catch (Exception e) {
            log.error("Error while sending HTML email", e);

            realtimeNotificationService.sendError(
                    "Send HTML Mail Failed",
                    "Unable to send HTML mail: " + e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NotificationResponse.error("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(
                "Mail Notification Service is up and running"
        );
    }
}
