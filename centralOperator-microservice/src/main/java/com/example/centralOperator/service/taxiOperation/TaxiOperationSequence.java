package com.example.centralOperator.service.taxiOperation;

import com.example.centralOperator.model.taxiOperation.TaxiOperationDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TaxiOperationSequence {

    // Thread-safe Map to store taxi operation sequences
    // TaxiOperationDTO is an interface implemented by IdlingOperationDTO, ReposOperationDTO, ServiceOperationDTO
    private final Map<String, Queue<TaxiOperationDTO>> taxiOperationSequence = new ConcurrentHashMap<>();

    public Queue<TaxiOperationDTO> getTaxiOperations(String taxiId) {
        return taxiOperationSequence.getOrDefault(taxiId, new ConcurrentLinkedQueue<>());
    }

    public void enqueueOperation(String taxiId, TaxiOperationDTO operation) {
        taxiOperationSequence
                .computeIfAbsent(taxiId, k -> new ConcurrentLinkedQueue<>())
                .offer(operation);
    }

    public TaxiOperationDTO dequeueOperation(String taxiId) {
        Queue<TaxiOperationDTO> queue = taxiOperationSequence.get(taxiId);
        return (queue != null) ? queue.poll() : null;
    }

    public int getQueueSize(String taxiId) {
        Queue<TaxiOperationDTO> queue = taxiOperationSequence.get(taxiId);
        return (queue != null) ? queue.size() : 0;
    }

    public void clearOperations(String taxiId) {
        taxiOperationSequence.remove(taxiId);
    }


    public void printTaxiOperationSequence() {
        if (taxiOperationSequence.isEmpty()) {
            System.out.print("Empty taxiOperationSequence.");
        } else {
            taxiOperationSequence.forEach((taxiId, queue) -> {
                System.out.print("Taxi " + taxiId + " sequence: ");
                queue.forEach(operation -> System.out.print(operation + " <- "));
                System.out.println("");
            });

        }

    }

    public void initializeMap() {
        this.taxiOperationSequence.clear();
    }
}