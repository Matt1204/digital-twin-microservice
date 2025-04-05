package com.example.centralOperator.service;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.model.CoResType;
import com.example.centralOperator.publisher.MessagePublisherService;
import com.example.centralOperator.service.monitoring.MatchingRateMonitorService;
import com.example.centralOperator.service.monitoring.UtilityMonitorService;
import com.example.centralOperator.service.taxiOperation.TaxiOperationSequence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CoInitService {
    @Autowired
    TaxiStateMap taxiStateMap;

    @Autowired
    ActiveOrders activeOrders;

    @Autowired
    TaxiOperationSequence taxiOperationSequence;

    @Autowired
    MessagePublisherService messagePublisherService;

    @Autowired
    UtilityMonitorService utilityMonitorService;

    @Autowired
    private MatchingRateMonitorService matchingRateMonitorService;

    private ObjectMapper objectMapper = new ObjectMapper();

    public void handleCoInit(String correlationId, JsonNode msgJsonNode) {
//        System.out.println("Handling CO_INIT with correlationId: " + correlationId);
        taxiStateMap.initializeMap();
        activeOrders.initializeActiveOrders();
        taxiOperationSequence.initializeMap();

//        taxiStateMap.printTaxisStateMap();
//        activeOrders.printActiveOrders();
//        taxiOperationSequence.printTaxiOperationSequence();


        utilityMonitorService.initializeUtilityLogging();
        matchingRateMonitorService.initMatchingRateLogging();

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("responseType", CoResType.CO_INIT_COMPLETE);
        try {
            String resJson = objectMapper.writeValueAsString(resMap);
            messagePublisherService.publishMessageWithCorrelationId(RabbitMQConfig.CO_RESPONSE_QUEUE, resJson, correlationId);
        } catch (JsonProcessingException e) {
            System.err.println("error parsing CO_INIT response: " + e);
        }
    }
}
