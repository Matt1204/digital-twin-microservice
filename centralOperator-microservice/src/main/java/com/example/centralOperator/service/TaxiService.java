package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiAction;
import com.example.centralOperator.model.TaxiState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TaxiService {
    private final Logger logger = LoggerFactory.getLogger(TaxiService.class);
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private double CHARGE_THRESHOLD = 20;

    /**
     * Process the incoming TaxiState.
     */
    public String getTaxiAction(TaxiState taxiState) {
        // sleep for 3 seconds

        try {
//            Thread.sleep(1000);
            double chargePossibility = random.nextDouble();
            int toTaxiZone;
            boolean isToCharge;

            if (chargePossibility < 0.1 || taxiState.getSoc() < this.CHARGE_THRESHOLD) {
                toTaxiZone = getRandomZone(List.of(1, 2, 3, 4, 5));
                isToCharge = true;
            } else {
                List<Integer> popularZones = List.of(79,
                        170, 48, 230, 142, 107, 68, 162,
                        141);
                toTaxiZone = getRandomZone(popularZones);
                isToCharge = false;
            }

            String resJson = objectMapper.writeValueAsString(new TaxiAction(toTaxiZone, isToCharge));
            return resJson;


        } catch (Exception e) {
            throw new RuntimeException("Error processing taxi status", e);
        }
    }

    private int getRandomZone(List<Integer> zones) {
        return zones.get(ThreadLocalRandom.current().nextInt(zones.size()));
    }
}

