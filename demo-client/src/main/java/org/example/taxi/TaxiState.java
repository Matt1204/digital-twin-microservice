package org.example.taxi;

public class TaxiState {
    private int timeInterval;
    private int taxiZone;
    private double soc;

    public TaxiState() {
    }

    public TaxiState(int timeInterval, int zone, double soc) {
        this.timeInterval = timeInterval;
        this.taxiZone = zone;
        this.soc = soc;
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

    @Override
    public String toString() {
        return "TaxiStatus{" +
                "timeInterval=" + timeInterval +
                ", taxiZone=" + taxiZone +
                ", soc=" + soc +
                '}';
    }
}

