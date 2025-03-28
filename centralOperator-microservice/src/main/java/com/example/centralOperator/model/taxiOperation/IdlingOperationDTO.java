package com.example.centralOperator.model.taxiOperation;

public class IdlingOperationDTO implements TaxiOperationDTO {
    private int idleTime;

    public IdlingOperationDTO() {}

    public IdlingOperationDTO(int idleTime) {
        this.idleTime = idleTime;
    }

    public int getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    @Override
    public String toString() {
        return "IdlingOperationDTO{" +
                "idleTime=" + idleTime +
                '}';
    }
}
