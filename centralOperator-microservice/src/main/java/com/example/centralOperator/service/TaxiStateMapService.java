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
public class TaxiStateMapService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(TaxiStateMapService.class);

    @Autowired
    private TaxiStateMap taxiStateMap;

    public void handleUpdateActiveTaxi(String jsonMsg) {
//        System.out.println("----- TaxiStateMap Update Start -----");
        try {
            Map<String, Object> parsedMap = objectMapper.readValue(
                    jsonMsg, new TypeReference<Map<String, Object>>() {
                    });

            if (parsedMap == null || !parsedMap.containsKey("taxiState")) {
                logger.error("Invalid input JSON: Missing taxiState.");
                return;
            }
            // boolean isToAdd = (Boolean) parsedMap.get("isToAdd");
            TaxiState taxiState = objectMapper.convertValue(parsedMap.get("taxiState"), TaxiState.class);
            taxiStateMap.addUpdateTaxi(taxiState);

//            taxiStateMap.printTaxisStateMap();
//            System.out.println("----- TaxiStateMap Update Done -----");
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert result to JSON", e);
            return;
        }
    }
}
