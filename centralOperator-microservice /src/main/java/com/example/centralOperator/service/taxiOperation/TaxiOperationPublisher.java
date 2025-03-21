package com.example.centralOperator.service.taxiOperation;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.model.CoResType;
import com.example.centralOperator.model.taxiOperation.IdlingOperationDTO;
import com.example.centralOperator.model.taxiOperation.ReposOperationDTO;
import com.example.centralOperator.model.taxiOperation.ServiceOperationDTO;
import com.example.centralOperator.model.taxiOperation.TaxiOperationType;
import com.example.centralOperator.publisher.MessagePublisherService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TaxiOperationPublisher {

    @Autowired
    private MessagePublisherService messagePublisherService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishIdlingOperation(String taxiId, IdlingOperationDTO operationData) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("responseType", CoResType.NEW_TAXI_OPERATION);

        Map<String, Object> payload = new HashMap<>();
        payload.put("operationType", TaxiOperationType.IDLING);
        payload.put("taxiId", taxiId);
        payload.put("operationData", operationData);
        messageData.put("payload", payload);

        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            messagePublisherService.publishMessage(RabbitMQConfig.CO_RESPONSE_QUEUE, msgJson);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing response: " + e.getMessage());
        }
    }

    public void publishReposOperation(String taxiId, ReposOperationDTO operationDTO) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("responseType", CoResType.NEW_TAXI_OPERATION);

        Map<String, Object> payload = new HashMap<>();
        payload.put("taxiId", taxiId);
        payload.put("operationType", TaxiOperationType.REPOSITIONING);
        payload.put("operationData", operationDTO);

        messageData.put("payload", payload);

        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            messagePublisherService.publishMessage(RabbitMQConfig.CO_RESPONSE_QUEUE, msgJson);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing response: " + e.getMessage());
        }
    }

    public void publishServiceOperation(String taxiId, ServiceOperationDTO operationDTO) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("responseType", CoResType.NEW_TAXI_OPERATION);

        Map<String, Object> payload = new HashMap<>();
        payload.put("taxiId", taxiId);
        payload.put("operationType", TaxiOperationType.SERVICE);
        payload.put("operationData", operationDTO);

        messageData.put("payload", payload);

        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            messagePublisherService.publishMessage(RabbitMQConfig.CO_RESPONSE_QUEUE, msgJson);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing response: " + e.getMessage());
        }
    }
}
