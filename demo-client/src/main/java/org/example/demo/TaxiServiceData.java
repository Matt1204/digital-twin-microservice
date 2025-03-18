package org.example.demo;

import org.example.order.TaxiOrder;

public class TaxiServiceData {
    private TaxiOrder taxiOrder;

    public TaxiServiceData(TaxiOrder taxiOrder) {
        this.taxiOrder = taxiOrder;
    }

    public void setTaxiOrder(TaxiOrder taxiOrder) {
        this.taxiOrder = taxiOrder;
    }

    public TaxiOrder getTaxiOrder() {
        return taxiOrder;
    }
}
