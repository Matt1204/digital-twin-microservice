package com.example.centralOperator.model.taxiOperation;

public class ReposOperationDTO {
    private double toLat;
    private double toLon;

    public ReposOperationDTO(){}

    public ReposOperationDTO(double toLat, double toLon) {
        this.toLat = toLat;
        this.toLon = toLon;
    }

    public double getToLat() {
        return toLat;
    }

    public double getToLon() {
        return toLon;
    }

    public void setToLat(double toLat) {
        this.toLat = toLat;
    }

    public void setToLon(double toLon) {
        this.toLon = toLon;
    }
}
