package com.example.centralOperator.service;
import com.example.centralOperator.model.TaxiState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TaxiStateMap {

    private final Map<String, TaxiState> taxiStateMap = new ConcurrentHashMap<>();

    // Retrieve all active taxis
    public List<TaxiState> getTaxiStateMap() {
        return List.copyOf(taxiStateMap.values());
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
        taxiStateMap.remove(taxiState.getTaxiId());
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
}