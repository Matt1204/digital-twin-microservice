package com.example.centralOperator.model.taxiOperation;

public class IdlingOperationDTO {
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
}
