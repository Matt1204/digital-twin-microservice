package org.example.taxi;

import java.io.Serializable;

public class TaxiState implements Serializable {
    private String taxiId;
    private int timeIntervalId;
    private int taxiZoneId;
    private double longitude;
    private double latitude;
    private double soc;



    public TaxiState() {
    }

    public TaxiState(String taxiId, int timeIntervalId, int taxiZoneId, double soc) {
        this(taxiId, timeIntervalId, taxiZoneId, soc, 0, 0);
    }

    public TaxiState(String taxiId, int timeIntervalId, int taxiZoneId, double soc, double longitude, double latitude) {
        this.taxiId = taxiId;
        this.timeIntervalId = timeIntervalId;
        this.taxiZoneId = taxiZoneId;
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

    public int getTaxiZoneId() {
        return taxiZoneId;
    }

    public void setTaxiZoneId(int taxiZoneId) {
        this.taxiZoneId = taxiZoneId;
    }

    public double getSoc() {
        return soc;
    }

    public void setSoc(double soc) {
        this.soc = soc;
    }

    public int getTimeIntervalId() {
        return timeIntervalId;
    }

    public void setTimeIntervalId(int timeIntervalId) {
        this.timeIntervalId = timeIntervalId;
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
                ", timeInterval=" + timeIntervalId +
                ", taxiZone=" + taxiZoneId +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", soc=" + soc +
                '}';
    }
}
