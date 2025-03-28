package com.example.centralOperator.service;
import com.example.centralOperator.model.TaxiState;
import com.example.centralOperator.model.taxiOperation.TaxiOperationType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TaxiStateMap {

    private final Map<String, TaxiState> taxiStateMap = new ConcurrentHashMap<>();

    // Retrieve all active taxis as a copy of the map
    public Map<String, TaxiState> getTaxiStateMap() {
        return new ConcurrentHashMap<>(taxiStateMap);
    }

    public Map<String, TaxiState> getIdlingTaxis() {
        return taxiStateMap.entrySet().stream()
                .filter(entry -> entry.getValue().getOperation() == TaxiOperationType.IDLING)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // Add or update a taxi in the map
    public void addUpdateTaxi(TaxiState taxiState) {
        taxiStateMap.put(taxiState.getTaxiId(), taxiState);
    }

    // Update a taxi's state
    public void addUpdateTaxiById(String taxiId, TaxiState newState) {
        taxiStateMap.put(taxiId, newState);
    }

    // Find a taxi by ID
    public TaxiState findTaxiById(String taxiId) {
        return taxiStateMap.get(taxiId);
    }

    // Remove a taxi from the map
    public void removeTaxi(TaxiState taxiState) {
        taxiStateMap.remove(taxiState.getTaxiId(), taxiState);
    }

    // Remove a taxi by ID
    public void removeTaxiById(String taxiId) {
        taxiStateMap.remove(taxiId);
    }

    public void removeListOfTaxis(List<String> taxiIdList) {
        taxiIdList.forEach(taxiStateMap::remove);
    }

    // Check if a taxi exists
    public boolean containsTaxi(String taxiId) {
        return taxiStateMap.containsKey(taxiId);
    }

    // Check if the map contains any TaxiState data
    public boolean hasAnyTaxiState() {
        return !taxiStateMap.isEmpty();
    }

    // Print all active taxis in a key-value format
    public void printTaxisStateMap() {
        if (taxiStateMap.isEmpty()) {
            System.out.println("No active taxis available.");
        } else {
            taxiStateMap.forEach((taxiId, taxiState) ->
                System.out.println(taxiId + " -> " + taxiState)
            );
        }
    }

    public List<String> getTaxiIdList() {
        return taxiStateMap.keySet().stream().collect(Collectors.toList());
    }

    public void initializeMap() {
        taxiStateMap.clear();
    }
}