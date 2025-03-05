package com.example.demo_microservice.model;

public class TaxiAction {
    private int taxiZoneId;
    private boolean isToCharge;

    public TaxiAction() { }

    public TaxiAction(int taxiZoneId) {
        this(taxiZoneId, false);
    }

    public TaxiAction(int taxiZoneId, boolean isToCharge) {
        this.taxiZoneId = taxiZoneId;
        this.isToCharge = isToCharge;
    }

    public void setTaxiZoneId(int taxiZoneId) {
        this.taxiZoneId = taxiZoneId;
    }

    public void setToCharge(boolean toCharge) {
        isToCharge = toCharge;
    }

    public int getTaxiZoneId() {
        return this.taxiZoneId;
    }

    public boolean getIsToCharge() {
        return this.isToCharge;
    }

    @Override
    public String toString() {
        return "Action("+ this.taxiZoneId+", " + this.isToCharge + ")";
    }
}
