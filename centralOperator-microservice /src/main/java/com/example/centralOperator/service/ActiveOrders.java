package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;

@Service
public class ActiveOrders {

    private final List<TaxiOrder> activeOrders = new CopyOnWriteArrayList<>();

    public List<TaxiOrder> getActiveOrders() {
        return List.copyOf(activeOrders);
    }

    public void addActiveOrder(TaxiOrder taxiOrder) {
        activeOrders.add(taxiOrder);
    }

    public void removeActiveOrder(TaxiOrder taxiOrder) {
        activeOrders.remove(taxiOrder);
    }

    public TaxiOrder findOrderById(String orderId) {
        return activeOrders.stream()
                .filter(order -> order.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
    }

    public void removeActiveOrderById(String orderId) {
        activeOrders.removeIf(order -> order.getOrderId().equals(orderId));
    }

    public void removeActiveOrderList(List<String> orderIdList){
        orderIdList.forEach(orderId -> {
            this.removeActiveOrderById(orderId);
        });
    }

    public String printActiveOrders() {
        if (activeOrders.isEmpty()) {
            return "No active orders available.";
        } else {
            StringBuilder content = new StringBuilder("Active Orders:\n");
            activeOrders.forEach(order -> content.append(order).append("\n"));
            return content.toString();
        }
    }

    public List<String> getActiveOrdersId() {
        List<String> orderIds = new ArrayList<>();
        for (TaxiOrder order : activeOrders) {
            orderIds.add(order.getOrderId());
        }
        return orderIds;
    }

    public int getActiveOrdersCount() {
        return activeOrders.size();
    }

    public TaxiOrder getRandomActiveOrder() {
        if (activeOrders.isEmpty()) {
            return null;
        }
        Random random = new Random();
        int index = random.nextInt(getActiveOrdersCount());
        return activeOrders.get(index);
    }
}