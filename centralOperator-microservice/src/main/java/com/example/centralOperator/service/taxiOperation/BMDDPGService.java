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

    /*
     * This is the entry point of the BMDDPG algorithm
     * this method will be invoked when every time windows begins
     * */
    public void handleTriggerAlgorithm() {
        if (!taxiStateMap.hasAnyTaxiState()) {
            System.err.println("No TaxiState for BMDDPG, abort");
            return;
        }
        // all the taxis to be repositioned/mathched
        Map<String, TaxiState> taxisToUse = taxiStateMap.getIdlingTaxis();
        // all the orders to be matched
        List<TaxiOrder> ordersToUse = activeOrders.getActiveOrders();

        // Calculate Repositioning Result.
        // write your own implementation here, just make sure the return type is same
        Map<String, ReposOperationDTO> repositionResult = handleRepositionAlgo(taxisToUse);

        // Calculate Repositioning Result.
        // write your own implementation here, just make sure the return type is same
        Map<String, List<String>> matchingResult = handleMatchingAlgo(taxisToUse, ordersToUse);

        // Not your concern
        handleReposResults(repositionResult);
//        handleMatchingResults(matchingResult);
        coMatchingService.handleMatchingResults(matchingResult);
        taxiOperationSequence.printTaxiOperationSequence();

        repositionResult.forEach((taxiId, val) -> {
            performOpInSequence(taxiId);
        });
    }

    /*
     * Implement your Repositioning algorithm here
     * you can have whatever implementation you want, just make sure the return data is same.
     * Return type is a Map:
     * Key: taxiId (String)
     * Value: ReposOperationDTO class. generated using coordinates of reposition destination
     * */
    private Map<String, ReposOperationDTO> handleRepositionAlgo(Map<String, TaxiState> taxiStateMap) {
        // remove dummy code below
        Map<String, ReposOperationDTO> repositionResults = new HashMap<>();

        taxiStateMap.entrySet().stream().forEach(entry -> {
            TaxiState taxiState = entry.getValue();
            String taxiId = entry.getKey();
            repositionResults.put(taxiId, this.demoGenerateReposOp());
        });

        return repositionResults;
    }

    /*
     * Implement your Match-Making algorithm here
     * you can have whatever implementation you want, just make sure the return data is same.
     * Return type is a Map, this Map must have 2 entries:
     * entry 1: key="matchedOrders", value = a list of orderId
     * entry 2: key="matchedTaxis", value = a list of taxiId
     * Example:
     * {
     *  "matchedOrders" => ["2", "6", "10"]
     *  "matchedTaxis" => ["46", "700", "9"]
     * }
     * order 2 match to taxi 46, order 6 match to taxi 700...
     *
     * So, matchedOrders and matchedTaxis should have exact same length
     * */
    private Map<String, List<String>> handleMatchingAlgo(Map<String, TaxiState> taxiStateMap, List<TaxiOrder> orderList) {
        List<TaxiState> taxiStateList = taxiStateMap.values().stream().toList();
        Map<String, List<String>> matchedResult = coMatchingService.demoSimpleMatch(taxiStateList, orderList);

        return matchedResult;
    }

    private void handleReposResults(Map<String, ReposOperationDTO> reposResults) {
        reposResults.forEach((taxiId, reposOperation) ->
                taxiOperationSequence.enqueueOperation(taxiId, reposOperation)
        );
        // taxiOperationSequence.printTaxiOperationSequence();
    }

    // 1 adding Operation to sequence
    // 2 removing orders from activeOrders
    private void handleMatchingResults(Map<String, List<String>> matchingResult) {
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

            taxiOperationSequence.enqueueOperation(taxiId, new ServiceOperationDTO(orderId));
        }

        activeOrders.removeActiveOrderList(matchedOrders);
    }

    public void handleTaxiOpDone(String taxiId, TaxiOperationType operationType) {
        if (taxiOperationSequence.getQueueSize(taxiId) != 0) {
            performOpInSequence(taxiId);
        } else {
//            System.out.println("No Op in sequence for taxi " + taxiId);
            switch (operationType) {
                case TaxiOperationType.IDLING -> {
//                    System.out.println("BMDDPG: handle Idling_done.");
                    handleIdlingDone(taxiId);
                    break;
                }
                case TaxiOperationType.REPOSITIONING -> {
//                    System.out.println("BMDDPG: handle Repositioning_done.");
                    handleRepositioningDone(taxiId);
                    break;
                }
                case TaxiOperationType.SERVICE -> {
                    handleServiceDone(taxiId);
//                    System.out.println("BMDDPG: handle Service_done.");
                    break;
                }
            }
        }
    }

    private void performOpInSequence(String taxiId) {
        System.out.print("taxi " + taxiId + " perform op from sequence: ");
//        taxiOperationSequence.printTaxiOperationSequence();
        TaxiOperationDTO taxiOperationDTO = taxiOperationSequence.dequeueOperation(taxiId);
//        taxiOperationSequence.printTaxiOperationSequence();

        if (taxiOperationDTO instanceof IdlingOperationDTO idlingOperationDTO) {
            // Handle idling operation
            System.out.print(idlingOperationDTO + "\n");
            taxiOperationPublisher.publishIdlingOperation(taxiId, idlingOperationDTO);
        } else if (taxiOperationDTO instanceof ReposOperationDTO reposOperationDTO) {
            // Handle repositioning operation
            System.out.print(reposOperationDTO + "\n");
            taxiOperationPublisher.publishReposOperation(taxiId, reposOperationDTO);
        } else if (taxiOperationDTO instanceof ServiceOperationDTO serviceOperationDTO) {
            // Handle service operation
            System.out.print(serviceOperationDTO + "\n");
            taxiOperationPublisher.publishServiceOperation(taxiId, serviceOperationDTO);
        } else {
            System.err.println("Unknown TaxiOperationDTO type");
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
//        taxiOperationPublisher.publishReposOperation(taxiId, demoGenerateReposOp());
        taxiOperationPublisher.publishIdlingOperation(taxiId, new IdlingOperationDTO(120));
    }

    private void handleRepositioningDone(String taxiId) {
        taxiOperationPublisher.publishIdlingOperation(taxiId, new IdlingOperationDTO(120));
//        TaxiOrder order = activeOrders.getRandomActiveOrder();
//        if (order != null) {
//            String matchedOrderId = order.getOrderId();
//            activeOrders.removeActiveOrder(order);
//
//            taxiOperationPublisher.publishServiceOperation(taxiId, new ServiceOperationDTO(matchedOrderId));
//
//        } else {
//            System.out.println("No active orders available for matchmaking.");
//            return;
//        }
    }

    private void handleServiceDone(String taxiId) {
        taxiOperationPublisher.publishIdlingOperation(taxiId, new IdlingOperationDTO(120));
    }

}
