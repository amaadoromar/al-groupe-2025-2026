package org.eSante.domain.models.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportData {

    private Long patientId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private String patientName;

    // Aggregated vital signs data
    private VitalSignsStats heartRateStats;
    private VitalSignsStats spo2Stats;
    private VitalSignsStats bloodPressureStats;
    private VitalSignsStats glucoseStats;
    private VitalSignsStats weightStats;

    // Alerts and events
    private List<AlertSummary> alerts;
    private int alertCount;
    private int emergencyCount;

    // Adherence
    private Map<String, Integer> adherenceBySensor;
    private double overallAdherenceRate;

    // Appointments
    private List<AppointmentSummary> appointments;

    // Constructors
    public ReportData() {}

    public ReportData(Long patientId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.patientId = patientId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters and Setters
    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public VitalSignsStats getHeartRateStats() {
        return heartRateStats;
    }

    public void setHeartRateStats(VitalSignsStats heartRateStats) {
        this.heartRateStats = heartRateStats;
    }

    public VitalSignsStats getSpo2Stats() {
        return spo2Stats;
    }

    public void setSpo2Stats(VitalSignsStats spo2Stats) {
        this.spo2Stats = spo2Stats;
    }

    public VitalSignsStats getBloodPressureStats() {
        return bloodPressureStats;
    }

    public void setBloodPressureStats(VitalSignsStats bloodPressureStats) {
        this.bloodPressureStats = bloodPressureStats;
    }

    public VitalSignsStats getGlucoseStats() {
        return glucoseStats;
    }

    public void setGlucoseStats(VitalSignsStats glucoseStats) {
        this.glucoseStats = glucoseStats;
    }

    public VitalSignsStats getWeightStats() {
        return weightStats;
    }

    public void setWeightStats(VitalSignsStats weightStats) {
        this.weightStats = weightStats;
    }

    public List<AlertSummary> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertSummary> alerts) {
        this.alerts = alerts;
    }

    public int getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(int alertCount) {
        this.alertCount = alertCount;
    }

    public int getEmergencyCount() {
        return emergencyCount;
    }

    public void setEmergencyCount(int emergencyCount) {
        this.emergencyCount = emergencyCount;
    }

    public Map<String, Integer> getAdherenceBySensor() {
        return adherenceBySensor;
    }

    public void setAdherenceBySensor(Map<String, Integer> adherenceBySensor) {
        this.adherenceBySensor = adherenceBySensor;
    }

    public double getOverallAdherenceRate() {
        return overallAdherenceRate;
    }

    public void setOverallAdherenceRate(double overallAdherenceRate) {
        this.overallAdherenceRate = overallAdherenceRate;
    }

    public List<AppointmentSummary> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<AppointmentSummary> appointments) {
        this.appointments = appointments;
    }
}
