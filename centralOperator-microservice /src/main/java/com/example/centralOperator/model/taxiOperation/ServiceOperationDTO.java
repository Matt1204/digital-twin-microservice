package com.example.centralOperator.model.taxiOperation;

public class ServiceOperationDTO {
    private String orderId;

    public ServiceOperationDTO(){}

    public ServiceOperationDTO(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
