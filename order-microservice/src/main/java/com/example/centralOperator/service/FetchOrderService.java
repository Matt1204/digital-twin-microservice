package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class FetchOrderService {

    @Autowired
    private AllOrders allOrders;

    private static final Logger logger = LoggerFactory.getLogger(FetchOrderService.class);

    public String handleFetchOrders(String reqJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            JsonNode jsonNode = objectMapper.readTree(reqJson);
            String dateString = jsonNode.get("curDatetime").asText();
            int timeWindow = jsonNode.get("timeWindow").asInt();
            Date startDatetime = sdf.parse(dateString);

            List<TaxiOrder> ordersFound = findOrderInWindow(startDatetime, timeWindow);

            // Manually format pickupTime before serialization
            List<Map<String, Object>> formattedOrders = new ArrayList<>();
            for (TaxiOrder order : ordersFound) {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("orderId", order.getOrderId());
                orderMap.put("pickupLon", order.getPickupLon());
                orderMap.put("pickupLat", order.getPickupLat());
                orderMap.put("dropoffLon", order.getDropoffLon());
                orderMap.put("dropoffLat", order.getDropoffLat());
                orderMap.put("pickupTime", sdf.format(order.getPickupTime())); // Format the date manually
                orderMap.put("tripDistance", order.getTripDistance());
                orderMap.put("tripIncome", order.getTripIncome());
                formattedOrders.add(orderMap);
            }
            String resString = objectMapper.writeValueAsString(formattedOrders);
            return resString;
        } catch (Exception e) {
            logger.error("Error processing fetch order request", e);
            return "[]";
        }
    }

    private List<TaxiOrder> findOrderInWindow(Date startDatetime, int timeWindow) {
        LocalTime curTime = startDatetime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        LocalTime endTime = curTime.plusSeconds(timeWindow);
        logger.info("startTime: " + curTime);
        logger.info("endTime: " + endTime);

        List<TaxiOrder> allOrders = this.allOrders.getAllOrders();
        logger.info("allOrders size: " + allOrders.size());

        List<TaxiOrder> ordersFound = new ArrayList<>();
        for (TaxiOrder order : allOrders) {
            if (order.getPickupTime() == null) continue;
            Date puDatetime = order.getPickupTime();
            LocalTime puTime = puDatetime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

            if (!puTime.isBefore(curTime) && puTime.isBefore(endTime)) {
                logger.info("HITTTTT: " + order.getPickupTime());
                ordersFound.add(order);
            }
        }
        return ordersFound;
    }
}
