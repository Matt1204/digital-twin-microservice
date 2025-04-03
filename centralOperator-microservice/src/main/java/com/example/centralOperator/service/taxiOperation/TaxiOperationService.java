package com.example.centralOperator.service.taxiOperation;

import com.example.centralOperator.config.TaxiOperationConfig;
import com.example.centralOperator.model.taxiOperation.TaxiOperationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaxiOperationService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaxiOperationConfig taxiOperationConfig;

    @Autowired
    private BMDDPGService bmddpgService;

    private static final Logger logger = LoggerFactory.getLogger(TaxiOperationService.class);


    public void onTaxiOpDone(JsonNode payloadNode) {
        try {
            String taxiId = payloadNode.get("taxiId").asText();
            TaxiOperationType operationType = TaxiOperationType.valueOf(payloadNode.get("operationType").asText());
//            System.out.println(String.format("OpDone: taxiId:%s, type:%s", taxiId, operationType));

            // Read config
            String configValue = taxiOperationConfig.getOperationAlgorithm();

            switch (configValue) {
                case "BMDDPG":
                    bmddpgService.handleTaxiOpDone(taxiId, operationType);
                    break;
                case "B":
                    System.out.println("Algo B");
                    break;
                default:
                    System.out.println("fallback Algo");
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to process CO request message", e);
        }
    }
}
