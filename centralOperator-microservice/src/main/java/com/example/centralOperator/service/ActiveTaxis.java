package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.model.TaxiState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ActiveTaxis {

    private final List<TaxiState> activeTaxis = new CopyOnWriteArrayList<>();

    // Retrieve all active taxis
    public List<TaxiState> getActiveTaxis() {
        return List.copyOf(activeTaxis); // Returns an unmodifiable copy for thread safety
    }

    // Add a new taxi to the active list
    public void addActiveTaxi(TaxiState taxiState) {
        activeTaxis.add(taxiState);
    }

    // Remove a taxi from the active list
    public void removeActiveTaxi(TaxiState taxiState) {
        activeTaxis.remove(taxiState);
    }

    // Find a taxi by ID (assuming TaxiState has an 'id' field)
    public TaxiState findTaxiById(String taxiId) {
        return activeTaxis.stream()
                .filter(taxi -> taxi.getTaxiId().equals(taxiId))
                .findFirst()
                .orElse(null);
    }

    // Remove a taxi by ID
    public void removeActiveTaxiById(String taxiId) {
        activeTaxis.removeIf(taxi -> taxi.getTaxiId().equals(taxiId));
    }

    public void removeActiveTaxiList(List<String> taxiIdList){
        taxiIdList.forEach(taxiId -> {
            this.removeActiveTaxiById(taxiId);
        });
    }

    // Print all active taxis
    public void printActiveTaxis() {
        if (activeTaxis.isEmpty()) {
            System.out.println("No active taxis available.");
        } else {
            // System.out.println("Active Taxis:");
            activeTaxis.forEach(taxi -> System.out.println(taxi.toString()));
        }
    }

    public List<String> getActiveTaxisId() {
        List<String> taxiIds = new ArrayList<>();
        for (TaxiState taxi : this.activeTaxis) {
            taxiIds.add(taxi.getTaxiId());
        }
        return taxiIds;
    }
}