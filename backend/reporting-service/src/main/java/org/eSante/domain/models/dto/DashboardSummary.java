package org.eSante.domain.models.dto;

import java.util.List;

public class DashboardSummary {
    private Long patientId;
    private DashboardPoint heartRate;
    private DashboardPoint spo2;
    private DashboardPoint glucose;
    private DashboardPoint weight;
    private DashboardPoint steps;
    private DashboardPoint bpSystolic;
    private DashboardPoint bpDiastolic;

    private List<DashboardPoint> seriesHeartRate;
    private List<DashboardPoint> seriesSpO2;
    private List<DashboardPoint> seriesBloodPressureSys;
    private List<DashboardPoint> seriesBloodPressureDia;
    private List<DashboardPoint> seriesGlucose;
    private List<DashboardPoint> seriesWeight;

    private Integer alertCount;
    private List<AlertSummary> recentAlerts;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public DashboardPoint getHeartRate() { return heartRate; }
    public void setHeartRate(DashboardPoint heartRate) { this.heartRate = heartRate; }

    public DashboardPoint getSpo2() { return spo2; }
    public void setSpo2(DashboardPoint spo2) { this.spo2 = spo2; }

    public DashboardPoint getGlucose() { return glucose; }
    public void setGlucose(DashboardPoint glucose) { this.glucose = glucose; }

    public DashboardPoint getWeight() { return weight; }
    public void setWeight(DashboardPoint weight) { this.weight = weight; }
    public DashboardPoint getSteps() { return steps; }
    public void setSteps(DashboardPoint steps) { this.steps = steps; }

    public DashboardPoint getBpSystolic() { return bpSystolic; }
    public void setBpSystolic(DashboardPoint bpSystolic) { this.bpSystolic = bpSystolic; }

    public DashboardPoint getBpDiastolic() { return bpDiastolic; }
    public void setBpDiastolic(DashboardPoint bpDiastolic) { this.bpDiastolic = bpDiastolic; }

    public List<DashboardPoint> getSeriesHeartRate() { return seriesHeartRate; }
    public void setSeriesHeartRate(List<DashboardPoint> seriesHeartRate) { this.seriesHeartRate = seriesHeartRate; }

    public List<DashboardPoint> getSeriesSpO2() { return seriesSpO2; }
    public void setSeriesSpO2(List<DashboardPoint> seriesSpO2) { this.seriesSpO2 = seriesSpO2; }
    public List<DashboardPoint> getSeriesBloodPressureSys() { return seriesBloodPressureSys; }
    public void setSeriesBloodPressureSys(List<DashboardPoint> s) { this.seriesBloodPressureSys = s; }
    public List<DashboardPoint> getSeriesBloodPressureDia() { return seriesBloodPressureDia; }
    public void setSeriesBloodPressureDia(List<DashboardPoint> s) { this.seriesBloodPressureDia = s; }
    public List<DashboardPoint> getSeriesGlucose() { return seriesGlucose; }
    public void setSeriesGlucose(List<DashboardPoint> seriesGlucose) { this.seriesGlucose = seriesGlucose; }
    public List<DashboardPoint> getSeriesWeight() { return seriesWeight; }
    public void setSeriesWeight(List<DashboardPoint> seriesWeight) { this.seriesWeight = seriesWeight; }

    public Integer getAlertCount() { return alertCount; }
    public void setAlertCount(Integer alertCount) { this.alertCount = alertCount; }

    public List<AlertSummary> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<AlertSummary> recentAlerts) { this.recentAlerts = recentAlerts; }
}
