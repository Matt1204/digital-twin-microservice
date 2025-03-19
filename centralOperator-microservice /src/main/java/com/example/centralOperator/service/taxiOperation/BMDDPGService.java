package com.example.centralOperator.service.taxiOperation;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.model.CoResType;
import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.model.taxiOperation.TaxiOperationType;
import com.example.centralOperator.publisher.MessagePublisherService;
import com.example.centralOperator.service.ActiveOrders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class BMDDPGService {

    @Autowired
    private MessagePublisherService messagePublisherService;

    @Autowired
    private ActiveOrders activeOrders;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleTaxiOpDone(String taxiId, TaxiOperationType operationType){
        switch (operationType){
            case TaxiOperationType.IDLING -> {
                System.out.println("BMDDPG: handle Idling_done.");
                handleIdlingDone(taxiId);
                break;
            }
            case TaxiOperationType.REPOSITIONING -> {
                System.out.println("BMDDPG: handle Repositioning_done.");
                handleRepositioningDone(taxiId);
                break;
            }
            case TaxiOperationType.SERVICE -> {
                handleServiceDone(taxiId);
                System.out.println("BMDDPG: handle Service_done.");
                break;
            }
        }
    }

    private void handleIdlingDone(String taxiId){
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("responseType", CoResType.NEW_TAXI_OPERATION);

        Map<String, Object> payload = new HashMap<>();
        payload.put("taxiId", taxiId);
        payload.put("operationType", TaxiOperationType.REPOSITIONING);

        // generate random coordinate in NYC
        double minLat = 40.477399; // Minimum latitude for NYC
        double maxLat = 40.917577; // Maximum latitude for NYC
        double minLon = -74.259090; // Minimum longitude for NYC
        double maxLon = -73.700272; // Maximum longitude for NYC
        double randomLat = minLat + (Math.random() * (maxLat - minLat));
        double randomLon = minLon + (Math.random() * (maxLon - minLon));
        payload.put("toLat", randomLat);
        payload.put("toLon", randomLon);

        messageData.put("payload", payload);

        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            messagePublisherService.publishMessage(RabbitMQConfig.CO_RESPONSE_QUEUE, msgJson);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing response: " + e.getMessage());
        }
    }

    private void handleRepositioningDone(String taxiId){
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("responseType", CoResType.NEW_TAXI_OPERATION);

        Map<String, Object> payload = new HashMap<>();
        payload.put("taxiId", taxiId);
        payload.put("operationType", TaxiOperationType.SERVICE);

        TaxiOrder order = activeOrders.getRandomActiveOrder();
        if (order != null) {
            String matchedOrderId = order.getOrderId();
            activeOrders.removeActiveOrder(order);
            payload.put("orderId", matchedOrderId);

        } else {
            System.out.println("No active orders available for matchmaking.");
            return;
        }

//        payload.put("idleTime", 60);

        messageData.put("payload", payload);
        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            messagePublisherService.publishMessage(RabbitMQConfig.CO_RESPONSE_QUEUE, msgJson);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing response: " + e.getMessage());
        }
    }

    private void handleServiceDone(String taxiId){
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("responseType", CoResType.NEW_TAXI_OPERATION);

        Map<String, Object> payload = new HashMap<>();
        payload.put("taxiId", taxiId);
        payload.put("operationType", TaxiOperationType.IDLING);

        payload.put("idleTime", 60);

        messageData.put("payload", payload);
        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            messagePublisherService.publishMessage(RabbitMQConfig.CO_RESPONSE_QUEUE, msgJson);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing response: " + e.getMessage());
        }
    }

}
