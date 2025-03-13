package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ActiveTaxisService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ActiveTaxisService.class);

    @Autowired
    private ActiveTaxis activeTaxis;

    public void handleUpdateActiveTaxi(String jsonMsg) {
        try {
            Map<String, Object> parsedMap = objectMapper.readValue(
                    jsonMsg, new TypeReference<Map<String, Object>>() {
                    });

            if (parsedMap == null || !parsedMap.containsKey("isToAdd") || !parsedMap.containsKey("taxiState")) {
                logger.error("Invalid input JSON: Missing isToAdd or taxiState.");
                return;
            }
            boolean isToAdd = (Boolean) parsedMap.get("isToAdd");
            TaxiState taxiState = objectMapper.convertValue(parsedMap.get("taxiState"), TaxiState.class);

            if (isToAdd) {
                activeTaxis.addActiveTaxi(taxiState);
            } else {
                activeTaxis.removeActiveTaxiById(taxiState.getTaxiId());
            }

            System.out.println("----- activeTaxis Update -----");
            activeTaxis.printActiveTaxis();
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert result to JSON", e);
            return;
        }
    }
}
