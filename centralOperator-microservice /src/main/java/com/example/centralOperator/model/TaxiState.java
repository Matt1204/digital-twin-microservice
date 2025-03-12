package com.example.centralOperator.model;

import java.io.Serializable;

public class TaxiState implements Serializable {
    private String taxiId;
    private int timeInterval;
    private int taxiZone;
    private double longitude;
    private double latitude;
    private double soc;



    public TaxiState() {
    }

    public TaxiState(String taxiId, int timeInterval, int zone, double soc) {
        this(taxiId, timeInterval, zone, soc, 0, 0);
    }

    public TaxiState(String taxiId, int timeInterval, int zone, double soc, double longitude, double latitude) {
        this.taxiId = taxiId;
        this.timeInterval = timeInterval;
        this.taxiZone = zone;
        this.soc = soc;
        this.longitude = longitude;
        this.latitude = latitude;
    }
    public String getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(String taxiId) {
        this.taxiId = taxiId;
    }

    public int getTaxiZone() {
        return taxiZone;
    }

    public void setTaxiZone(int taxiZone) {
        this.taxiZone = taxiZone;
    }

    public double getSoc() {
        return soc;
    }

    public void setSoc(double soc) {
        this.soc = soc;
    }

    public int getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(int timeInterval) {
        this.timeInterval = timeInterval;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "TaxiState{" +
                "taxiId='" + taxiId + '\'' +
                ", timeInterval=" + timeInterval +
                ", taxiZone=" + taxiZone +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", soc=" + soc +
                '}';
    }
}
