package com.example.centralOperator.listener;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.service.ActiveTaxisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CoTaxiUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(CoTaxiUpdateListener.class);

    @Autowired
    private ActiveTaxisService activeTaxisService;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.CO_ACTIVE_TAXIS_UPDATE_QUEUE)
    public void listenTaxiUpdate(Message message) {
        try {
            String jsonString = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("Received taxi update JSON: {}", jsonString);

            activeTaxisService.handleUpdateActiveTaxi(jsonString);

        } catch (Exception e) {
            logger.error("Failed to process taxi update message", e);
        }
    }
}
