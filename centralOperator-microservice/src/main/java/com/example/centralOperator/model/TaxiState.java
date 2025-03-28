package com.example.centralOperator.model;

import com.example.centralOperator.model.taxiOperation.TaxiOperationType;

import java.io.Serializable;

public class TaxiState implements Serializable {
    private String taxiId;
    private double longitude;
    private double latitude;
    private double revenue;
    private TaxiOperationType operation;

    private int timeIntervalId;
    private int taxiZoneId;
    private double soc; // State of Charge (battery level)

    public TaxiState() {
    }

    public TaxiState(String taxiId, double soc) {
        this(taxiId, 0, 0, 0, 0, soc, 0, TaxiOperationType.OTHER);
    }

    public TaxiState(String taxiId,
                     double longitude,
                     double latitude,
                     double soc,
                     double revenue,
                     TaxiOperationType operation) {
        this(taxiId, longitude, latitude, 0, 0, soc, revenue, operation);
    }

//    public TaxiState(String taxiId, double longitude, double latitude) {
//        this(taxiId, longitude, latitude, 0, 0, 0, 0, TaxiOperationType.OTHER);
//    }


    public TaxiState(
            String taxiId,
            double longitude,
            double latitude,
            int timeIntervalId,
            int taxiZoneId,
            double soc,
            double revenue,
            TaxiOperationType operation) {
        this.taxiId = taxiId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.timeIntervalId = validateNonNegative(timeIntervalId, "Time interval ID");
        this.taxiZoneId = validateNonNegative(taxiZoneId, "Taxi zone ID");
        this.soc = validateSoc(soc);
        this.operation = operation;
        this.revenue = revenue;
    }

    /**
     * Getters and Setters with Validation
     **/

    public String getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(String taxiId) {
        this.taxiId = taxiId;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public int getTimeIntervalId() {
        return timeIntervalId;
    }

    public void setTimeIntervalId(int timeIntervalId) {
        this.timeIntervalId = validateNonNegative(timeIntervalId, "Time interval ID");
    }

    public int getTaxiZoneId() {
        return taxiZoneId;
    }

    public void setTaxiZoneId(int taxiZoneId) {
        this.taxiZoneId = validateNonNegative(taxiZoneId, "Taxi zone ID");
    }

    public double getSoc() {
        return soc;
    }

    public void setSoc(double soc) {
        this.soc = validateSoc(soc);
    }

    public double getRevenue() {
        return revenue;
    }

    public TaxiOperationType getOperation() {
        return operation;
    }

    public void setOperation(TaxiOperationType operation) {
        this.operation = operation;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    @Override
    public String toString() {
        return "TaxiState{" +
                "taxiId='" + taxiId + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", revenue=" + revenue +
                ", operation=" + operation +
                ", timeIntervalId=" + timeIntervalId +
                ", taxiZoneId=" + taxiZoneId +
                ", soc=" + soc +
                '}';
    }

    /**
     * Helper Methods
     **/

    private int validateNonNegative(int value, String fieldName) {
        return Math.max(value, 0);
    }

    private double validateSoc(double soc) {
        return Math.min(Math.max(soc, 0), 100);
    }
}