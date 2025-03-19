package org.example.demo;

import java.io.Serializable;

public class TaxiIdlingData implements Serializable {

    private int idleTime;

    public TaxiIdlingData(int idleTime) {
        this.idleTime = idleTime;
    }

    public int getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }
}