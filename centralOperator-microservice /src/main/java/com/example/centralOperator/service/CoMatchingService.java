package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.model.TaxiState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CoMatchingService {

    @Autowired
    private ActiveTaxis activeTaxis;

    @Autowired
    private ActiveOrders activeOrders;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(CoMatchingService.class);

    public String matchTaxiToOrder(String jsonMaps) {
        try {
            // Parse JSON into a map
            Map<String, Object> parsedMaps = objectMapper.readValue(jsonMaps, new TypeReference<Map<String, Object>>() {
            });
            if (parsedMaps == null || !parsedMaps.containsKey("activeOrders") || !parsedMaps.containsKey("activeTaxis")) {
                logger.error("Invalid input JSON: Missing activeOrders or activeTaxis.");
                return "{}"; // Return empty JSON object
            }

            // Convert "activeOrders" to Map<String, Deque<Integer>>
            Map<String, Deque<Integer>> activeOrdersMap = new HashMap<>();
            Map<String, List<Integer>> orders = objectMapper.convertValue(parsedMaps.get("activeOrders"), new TypeReference<Map<String, List<Integer>>>() {
            });
            if (orders != null) {
                for (Map.Entry<String, List<Integer>> entry : orders.entrySet()) {
                    activeOrdersMap.put(entry.getKey(), new ArrayDeque<>(entry.getValue()));
                }
            }

            // Convert "activeTaxis" to Map<String, Set<Integer>>
            Map<String, Set<Integer>> activeTaxisMap = new HashMap<>();
            Map<String, List<Integer>> taxis = objectMapper.convertValue(parsedMaps.get("activeTaxis"), new TypeReference<Map<String, List<Integer>>>() {
            });
            if (taxis != null) {
                for (Map.Entry<String, List<Integer>> entry : taxis.entrySet()) {
                    activeTaxisMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
            }

            // Perform taxi-to-order matching
            List<Integer> matchedOrders = new ArrayList<>();
            List<Integer> matchedTaxis = new ArrayList<>();

            for (String zone : activeOrdersMap.keySet()) {
                Deque<Integer> orderQueue = activeOrdersMap.get(zone);
                Set<Integer> taxiSet = activeTaxisMap.getOrDefault(zone, Collections.emptySet());
                Iterator<Integer> taxiIterator = taxiSet.iterator();

                while (!orderQueue.isEmpty() && taxiIterator.hasNext()) {
                    int orderId = orderQueue.poll();
                    int taxiId = taxiIterator.next();

                    logger.info("Zone {} - taxi:{} → order:{}", zone, taxiId, orderId);

                    matchedOrders.add(orderId);
                    matchedTaxis.add(taxiId);
                }
            }

            // Prepare result
            Map<String, List<Integer>> result = new HashMap<>();
            result.put("matchedOrders", matchedOrders);
            result.put("matchedTaxis", matchedTaxis);

            // Convert to JSON and return
            String resJson = objectMapper.writeValueAsString(result);
            return resJson;

        } catch (JsonProcessingException e) {
            logger.error("Failed to convert result to JSON", e);
            return "{}"; // Return empty JSON object on error
        } catch (Exception e) {
            logger.error("Failed to match taxis to orders", e);
            return "{}"; // Return empty JSON object on error
        }
    }

    public Map<String, List<String>> demoSimpleMatch(List<TaxiState> taxiList, List<TaxiOrder> orderList) {
        System.out.println("----------- simpleMatch() -----------");
//        List<TaxiOrder> orderList = activeOrders.getActiveOrders();
//        List<TaxiState> taxiList = activeTaxis.getActiveTaxis();
//        System.out.println("activeTaxis: " + activeTaxis.getActiveTaxisId());
//        System.out.println("activeOrders: " + activeOrders.getActiveOrdersId());

        Iterator<TaxiOrder> orderIterator = orderList.iterator();
        Iterator<TaxiState> taxiIterator = taxiList.iterator();
        // Perform taxi-to-order matching
        List<String> matchedOrdersId = new ArrayList<>();
        List<String> matchedTaxisId = new ArrayList<>();
        while (orderIterator.hasNext() && taxiIterator.hasNext()) {
            TaxiOrder order = orderIterator.next();
            TaxiState taxi = taxiIterator.next();

            matchedOrdersId.add(order.getOrderId());
            matchedTaxisId.add(taxi.getTaxiId());
            System.out.println(String.format("Taxi {} --> Order {}", taxi.getTaxiId(), order.getOrderId()));
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("matchedOrders", matchedOrdersId);
        result.put("matchedTaxis", matchedTaxisId);

        return result;

    }
}