package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActiveOrdersService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ActiveOrdersService.class);

    @Autowired
    private ActiveOrders activeOrders;

    public void handleAddActiveOrders(String jsonMsg) {
        try {
            Map<String, List<TaxiOrder>> parsedMap = objectMapper.readValue(jsonMsg, new TypeReference<Map<String, List<TaxiOrder>>>() {
            });
            if (parsedMap == null || !parsedMap.containsKey("orderList")) {
                logger.error("Invalid input JSON: Missing orderList.");
                return;
            }

            List<TaxiOrder> orderList = parsedMap.get("orderList");
            if (orderList == null || orderList.isEmpty()) {
                logger.warn("Received empty orderList. No orders added.");
                return;
            }

            orderList.forEach(order -> activeOrders.addActiveOrder(order));
            System.out.println("----- activeOrders Update -----");
            activeOrders.printActiveOrders();

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse input JSON", e);
        }
    }
}
