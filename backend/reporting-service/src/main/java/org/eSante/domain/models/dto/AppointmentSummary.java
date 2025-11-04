package org.eSante.domain.models.dto;

import java.time.LocalDateTime;

public class AppointmentSummary {
    private Long id;
    private LocalDateTime date;
    private String type;
    private String comment;

    public AppointmentSummary() {}

    public AppointmentSummary(Long id, LocalDateTime date, String type, String comment) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
