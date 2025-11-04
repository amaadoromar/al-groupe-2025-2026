package org.eSante.domain.models.dto;

public class VitalSignsStats {
    private String metric;
    private Double average;
    private Double median;
    private Double min;
    private Double max;
    private Double standardDeviation;
    private Double p10;
    private Double p90;
    private Integer measurementCount;
    private String unit;

    // For blood glucose
    private Double timeInRange; // % of time within 70–180 mg/dL

    // For blood pressure
    private Double averageSystolic;
    private Double averageDiastolic;
    private Double averageMAP; // Mean Arterial Pressure

    // Constructors
    public VitalSignsStats() {}

    public VitalSignsStats(String metric, String unit) {
        this.metric = metric;
        this.unit = unit;
    }

    // Getters and Setters
    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Double getAverage() {
        return average;
    }

    public void setAverage(Double average) {
        this.average = average;
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(Double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public Double getP10() {
        return p10;
    }

    public void setP10(Double p10) {
        this.p10 = p10;
    }

    public Double getP90() {
        return p90;
    }

    public void setP90(Double p90) {
        this.p90 = p90;
    }

    public Integer getMeasurementCount() {
        return measurementCount;
    }

    public void setMeasurementCount(Integer measurementCount) {
        this.measurementCount = measurementCount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getTimeInRange() {
        return timeInRange;
    }

    public void setTimeInRange(Double timeInRange) {
        this.timeInRange = timeInRange;
    }

    public Double getAverageSystolic() {
        return averageSystolic;
    }

    public void setAverageSystolic(Double averageSystolic) {
        this.averageSystolic = averageSystolic;
    }

    public Double getAverageDiastolic() {
        return averageDiastolic;
    }

    public void setAverageDiastolic(Double averageDiastolic) {
        this.averageDiastolic = averageDiastolic;
    }

    public Double getAverageMAP() {
        return averageMAP;
    }

    public void setAverageMAP(Double averageMAP) {
        this.averageMAP = averageMAP;
    }
}
