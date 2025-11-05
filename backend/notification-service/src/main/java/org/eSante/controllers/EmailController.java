package org.eSante.controllers;

import org.eSante.interfaces.EmailService;
import org.eSante.interfaces.RealtimeNotificationService;
import org.eSante.services.NotificationHistoryService;
import org.eSante.model.EmailNotificationLog;
import org.eSante.model.EmailNotificationRequest;
import org.eSante.model.NotificationResponse;
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
    private final NotificationHistoryService historyService;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendEmail(
            @Valid @RequestBody EmailNotificationRequest request
            ) {
        try {
            log.info("Received email notification request to: {}", request.getTo());

            emailService.sendSimpleMail(request);
            historyService.addEmail(new EmailNotificationLog(
                    java.time.Instant.now(),
                    request.getTo(),
                    request.getSubject(),
                    request.getMessage(),
                    request.getPatientId(),
                    request.getAuthorEmail()
            ));

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

    @GetMapping("/history")
    public ResponseEntity<java.util.List<EmailNotificationLog>> history(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "authorEmail", required = false) String authorEmail,
            @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit
    ) {
        int lim = (limit != null && limit > 0) ? Math.min(limit, 200) : 20;
        return ResponseEntity.ok(historyService.listEmails(patientId, to, authorEmail, lim));
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
