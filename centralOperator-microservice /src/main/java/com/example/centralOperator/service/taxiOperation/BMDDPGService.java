package com.example.centralOperator.service.taxiOperation;

import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.model.TaxiState;
import com.example.centralOperator.model.taxiOperation.*;
import com.example.centralOperator.publisher.MessagePublisherService;
import com.example.centralOperator.service.ActiveOrders;
import com.example.centralOperator.service.CoMatchingService;
import com.example.centralOperator.service.TaxiStateMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class BMDDPGService {

    @Autowired
    private MessagePublisherService messagePublisherService;

    @Autowired
    private TaxiOperationPublisher taxiOperationPublisher;

    @Autowired
    private ActiveOrders activeOrders;

    @Autowired
    private TaxiStateMap taxiStateMap;

    @Autowired
    private TaxiOperationSequence taxiOperationSequence;

    @Autowired
    private CoMatchingService coMatchingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleTriggerAlgorithm() {

        Map<String, TaxiState> taxisToUse = taxiStateMap.getIdlingTaxis();
        List<TaxiOrder> ordersToUse = activeOrders.getActiveOrders();

        // Generate Repositioning Result
        Map<String, ReposOperationDTO> repositionResult = handleRepositionAlgo(taxisToUse);

        // Generating Matching Result
        Map<String, List<String>> matchingResult = handleMatchingAlgo(taxisToUse, ordersToUse);

        handleReposResults(repositionResult);
        handleMatchingResults(matchingResult);
        taxiOperationSequence.printTaxiOperationSequence();
    }

    private Map<String, ReposOperationDTO> handleRepositionAlgo(Map<String, TaxiState> taxiStateMap) {
        Map<String, ReposOperationDTO> repositionResults = new HashMap<>();

        taxiStateMap.entrySet().stream().forEach(entry -> {
            TaxiState taxiState = entry.getValue();
            String taxiId = entry.getKey();
            repositionResults.put(taxiId, this.demoGenerateReposOp());
        });

        return repositionResults;
    }

    private Map<String, List<String>> handleMatchingAlgo(Map<String, TaxiState> taxiStateMap, List<TaxiOrder> orderList) {
        List<TaxiState> taxiStateList = taxiStateMap.values().stream().toList();
        Map<String, List<String>> matchedResult = coMatchingService.demoSimpleMatch(taxiStateList, orderList);

        return matchedResult;
    }

    private void handleReposResults(Map<String, ReposOperationDTO> reposResults) {
        reposResults.forEach((taxiId, reposOperation) ->
                taxiOperationSequence.enqueueOperation(taxiId, reposOperation)
        );
        taxiOperationSequence.printTaxiOperationSequence();
    }

    private void handleMatchingResults(Map<String, List<String>> matchingResult) {
        // 1 adding Operation to sequence
        // 2 removing orders from activeOrders
        if (!matchingResult.containsKey("matchedOrders") || !matchingResult.containsKey("matchedTaxis")) {
            System.err.println("handleMatchingResults() wrong data structure given");
        }

        List<String> matchedTaxis = matchingResult.get("matchedTaxis");
        List<String> matchedOrders = matchingResult.get("matchedOrders");

        if (matchedOrders.isEmpty() || matchedTaxis.isEmpty()) {
            System.err.println("handleMatchingResults() empty data given");
            return;
        }

        Iterator<String> ordersIterator = matchedOrders.iterator();
        Iterator<String> taxisIterator = matchedTaxis.iterator();

        while (ordersIterator.hasNext() && taxisIterator.hasNext()) {
            String orderId = ordersIterator.next();
            String taxiId = taxisIterator.next();

            System.out.println(String.format("! matched: taxi[%s] --> order[%s]", taxiId, orderId));

            taxiOperationSequence.enqueueOperation(taxiId, new ServiceOperationDTO(orderId));
        }

        activeOrders.removeActiveOrderList(matchedOrders);
    }

    public void handleTaxiOpDone(String taxiId, TaxiOperationType operationType) {
        if (taxiOperationSequence.getQueueSize(taxiId) != 0) {
            TaxiOperationDTO nextOp = taxiOperationSequence.dequeueOperation(taxiId);
            // !!!!!!!!!!!!!!!!!!!!!!!!!
            // perform nextOp

        }

        switch (operationType) {
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

    private ReposOperationDTO demoGenerateReposOp() {
        double minLat = 40.477399; // Minimum latitude for NYC
        double maxLat = 40.917577; // Maximum latitude for NYC
        double minLon = -74.259090; // Minimum longitude for NYC
        double maxLon = -73.700272; // Maximum longitude for NYC
        double randomLat = minLat + (Math.random() * (maxLat - minLat));
        double randomLon = minLon + (Math.random() * (maxLon - minLon));

        return new ReposOperationDTO(randomLat, randomLon);
    }

    private void handleIdlingDone(String taxiId) {
        // generate random coordinate in NYC. DUMMY data
        double minLat = 40.477399; // Minimum latitude for NYC
        double maxLat = 40.917577; // Maximum latitude for NYC
        double minLon = -74.259090; // Minimum longitude for NYC
        double maxLon = -73.700272; // Maximum longitude for NYC
        double randomLat = minLat + (Math.random() * (maxLat - minLat));
        double randomLon = minLon + (Math.random() * (maxLon - minLon));

        taxiOperationPublisher.publishReposOperation(taxiId, new ReposOperationDTO(randomLat, randomLon));
    }

    private void handleRepositioningDone(String taxiId) {

        TaxiOrder order = activeOrders.getRandomActiveOrder();
        if (order != null) {
            String matchedOrderId = order.getOrderId();
            activeOrders.removeActiveOrder(order);

            taxiOperationPublisher.publishServiceOperation(taxiId, new ServiceOperationDTO(matchedOrderId));

        } else {
            System.out.println("No active orders available for matchmaking.");
            return;
        }
    }

    private void handleServiceDone(String taxiId) {
        IdlingOperationDTO idlingOperationDTO = new IdlingOperationDTO(60);

        taxiOperationPublisher.publishIdlingOperation(taxiId, idlingOperationDTO);
    }

}
