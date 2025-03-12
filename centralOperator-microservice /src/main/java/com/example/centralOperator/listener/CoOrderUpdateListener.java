package com.example.centralOperator.listener;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.model.TaxiState;
import com.example.centralOperator.service.ActiveOrdersService;
import com.example.centralOperator.service.ActiveTaxis;
import com.example.centralOperator.service.ActiveTaxisService;
import com.example.centralOperator.service.CoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CoOrderUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(CoOrderUpdateListener.class);

    @Autowired
    private ActiveOrdersService activeOrdersService;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.CO_ACTIVE_ORDERS_UPDATE_QUEUE)
    public void listenOrderUpdate(Message message) {
        try {
            String jsonString = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("Received orders update JSON: {}", jsonString);

            activeOrdersService.handleAddActiveOrders(jsonString);

        } catch (Exception e) {
            logger.error("Failed to process taxi update message", e);
        }
    }
}
