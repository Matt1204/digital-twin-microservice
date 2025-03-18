package org.example.demo;

public class TaxiRepositionData {
    private double toLon;
    private double toLat;

    public TaxiRepositionData(double toLon, double toLat) {
        this.toLon = toLon;
        this.toLat = toLat;
    }

    public double getToLon() {
        return toLon;
    }

    public double getToLat() {
        return toLat;
    }

    public void setToLat(double toLat) {
        this.toLat = toLat;
    }

    public void setToLon(double toLon) {
        this.toLon = toLon;
    }
}
