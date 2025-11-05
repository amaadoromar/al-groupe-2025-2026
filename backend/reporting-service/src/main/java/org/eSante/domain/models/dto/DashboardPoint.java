package org.eSante.domain.models.dto;

public class DashboardPoint {
    private String time; // ISO-8601
    private Double value;

    public DashboardPoint() {}
    public DashboardPoint(String time, Double value) {
        this.time = time;
        this.value = value;
    }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
}

