package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.model.TaxiState;
import com.example.centralOperator.model.taxiOperation.ServiceOperationDTO;
import com.example.centralOperator.service.taxiOperation.TaxiOperationSequence;
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

    @Autowired
    private TaxiOperationSequence taxiOperationSequence;

    @Autowired
    private UtilityMonitorService utilityMonitorService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(CoMatchingService.class);

    public void handleMatchingResults(Map<String, List<String>> matchingResult) {
        if (!matchingResult.containsKey("matchedOrders") || !matchingResult.containsKey("matchedTaxis")) {
            System.err.println("handleMatchingResults() wrong data structure given");
        }
        List<String> matchedTaxis = matchingResult.get("matchedTaxis");
        List<String> matchedOrders = matchingResult.get("matchedOrders");

        if (matchedOrders.isEmpty() || matchedTaxis.isEmpty()) {
            System.err.println("handleMatchingResults() empty data given");
            return;
        }

        System.out.println("----------- handleMatchingResults() -----------");
        Iterator<String> ordersIterator = matchedOrders.iterator();
        Iterator<String> taxisIterator = matchedTaxis.iterator();
        while (ordersIterator.hasNext() && taxisIterator.hasNext()) {
            String orderId = ordersIterator.next();
            String taxiId = taxisIterator.next();
            System.out.println(String.format("! matched: taxi[%s] --> order[%s]", taxiId, orderId));

            utilityMonitorService.calculateUtility(taxiId, orderId);

            taxiOperationSequence.enqueueOperation(taxiId, new ServiceOperationDTO(orderId));
        }

        activeOrders.removeActiveOrderList(matchedOrders);
    }

    public Map<String, List<String>> demoSimpleMatch(List<TaxiState> taxiList, List<TaxiOrder> orderList) {
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
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("matchedOrders", matchedOrdersId);
        result.put("matchedTaxis", matchedTaxisId);

        return result;

    }
}