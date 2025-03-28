package com.example.centralOperator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaxiOperationConfig {

    @Value("${taxi.operation.algorithm:FALLBACK}") // Default to FALLBACK if not set
    private String operationAlgorithm;

    public String getOperationAlgorithm() {
        return operationAlgorithm;
    }
}