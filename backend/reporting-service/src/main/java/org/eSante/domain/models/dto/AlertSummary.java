package org.eSante.domain.models.dto;

import java.time.LocalDateTime;

public class AlertSummary {
    private Long id;
    private String alertType;
    private String level;
    private String message;
    private LocalDateTime createdAt;
    private String status;

    public AlertSummary() {}

    public AlertSummary(Long id, String alertType, String level,
                        String message, LocalDateTime createdAt, String status) {
        this.id = id;
        this.alertType = alertType;
        this.level = level;
        this.message = message;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
