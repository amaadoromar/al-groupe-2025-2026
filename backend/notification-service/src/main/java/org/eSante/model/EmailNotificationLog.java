package org.eSante.model;

import java.time.Instant;

public class EmailNotificationLog {
    private final Instant timestamp;
    private final String to;
    private final String subject;
    private final String message;
    private final Long patientId;
    private final String authorEmail;

    public EmailNotificationLog(Instant timestamp, String to, String subject, String message, Long patientId, String authorEmail) {
        this.timestamp = timestamp;
        this.to = to;
        this.subject = subject;
        this.message = message;
        this.patientId = patientId;
        this.authorEmail = authorEmail;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public Long getPatientId() { return patientId; }
    public String getAuthorEmail() { return authorEmail; }
}

