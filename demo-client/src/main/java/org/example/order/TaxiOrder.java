package org.example.order;

import java.util.Date;

public class TaxiOrder {
    String orderId;
    //    Rider rider;
//    GISPoint pickupLocation;
//    GISPoint dropoffLocation;
    double pickupLon;
    double pickupLat;
    double dropoffLon;
    double dropoffLat;
    Date pickupTime;
    int pickupZone;
    double tripDistance;
    double tripIncome;

    public TaxiOrder(String orderId, double pickupLon, double pickupLat, double dropoffLon, double dropoffLat, Date pickupTime, double tripDistance, double tripIncome) {
        this(orderId, pickupLon, pickupLat, dropoffLon, dropoffLat, pickupTime, tripDistance, tripIncome, 0);
    }

    public TaxiOrder(String orderId, double pickupLon, double pickupLat, double dropoffLon, double dropoffLat, Date pickupTime, double tripDistance, double tripIncome, int pickupZone) {
        this.orderId = orderId;
        this.pickupLon = pickupLon;
        this.pickupLat = pickupLat;
        this.dropoffLon = dropoffLon;
        this.dropoffLat = dropoffLat;
        this.pickupTime = pickupTime;
        this.tripDistance = tripDistance;
        this.tripIncome = tripIncome;
        this.pickupZone = pickupZone;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setPickupLon(double pickupLon) {
        this.pickupLon = pickupLon;
    }

    public void setPickupLat(double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public void setDropoffLon(double dropoffLon) {
        this.dropoffLon = dropoffLon;
    }

    public void setDropoffLat(double dropoffLat) {
        this.dropoffLat = dropoffLat;
    }

    public void setPickupTime(Date pickupTime) {
        this.pickupTime = pickupTime;
    }

    public void setPickupZone(int pickupZone) {
        this.pickupZone = pickupZone;
    }

    public void setTripDistance(double tripDistance) {
        this.tripDistance = tripDistance;
    }

    public void setTripIncome(double tripIncome) {
        this.tripIncome = tripIncome;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getPickupLon() {
        return pickupLon;
    }

    public double getPickupLat() {
        return pickupLat;
    }

    public double getDropoffLon() {
        return dropoffLon;
    }

    public double getDropoffLat() {
        return dropoffLat;
    }

    public Date getPickupTime() {
        return pickupTime;
    }

    public int getPickupZone() {
        return pickupZone;
    }

    public double getTripDistance() {
        return tripDistance;
    }

    public double getTripIncome() {
        return tripIncome;
    }

    @Override
    public String toString() {
        return "TaxiOrder{" +
                "orderId=" + orderId +
                ", pickupLon=" + pickupLon +
                ", pickupLat=" + pickupLat +
                ", dropoffLon=" + dropoffLon +
                ", dropoffLat=" + dropoffLat +
                ", pickupTime=" + pickupTime +
                ", pickupZone=" + pickupZone +
                ", tripDistance=" + tripDistance +
                ", tripIncome=" + tripIncome +
                '}';
    }
}

