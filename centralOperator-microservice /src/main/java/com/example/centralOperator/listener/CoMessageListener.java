package com.example.centralOperator.listener;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.model.TaxiState;
import com.example.centralOperator.service.CoService;
import com.example.centralOperator.service.TaxiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CoMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CoMessageListener.class);

    @Autowired
    private TaxiService taxiService;

    @Autowired
    private CoService coService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @Autowired
    private ObjectMapper objectMapper;  // Injected for efficiency

    /**
     * Listen to messages from the "taxi.requests" queue
     * and publish the results to "taxi.responses" queue.
     */
    @RabbitListener(queues = RabbitMQConfig.CO_MATCH_REQUEST_QUEUE)
    public void listenMatchReq(Message message) {
        MessageProperties props = message.getMessageProperties();
        try {
            String jsonString = new String(message.getBody(), StandardCharsets.UTF_8); // take message body as json string
            logger.info("**Received JSON string: {}", jsonString);

            // Retrieve and validate correlation ID
            String correlationId = props.getCorrelationId();
            if (correlationId == null || correlationId.isEmpty()) {
                logger.warn("**Received message without correlation ID");
                return;
            }

            String resJson = coService.matchTaxiToOrder(jsonString);

            // Send response using RabbitTemplate's JSON conversion
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CO_EXCHANGE, // exchange
                    RabbitMQConfig.CO_MATCH_RESPONSE_QUEUE, // routing_key
                    resJson,
                    messagePostProcessor -> {
                        messagePostProcessor.getMessageProperties().setCorrelationId(correlationId);
                        messagePostProcessor.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return messagePostProcessor;
                    }
            );
            logger.info("**Response sent json: {}", resJson);
            logger.info("**Response sent with correlation ID: {}", correlationId);


        } catch (Exception e) {
            logger.error("Failed to process message: {}", e.getMessage(), e);
        }
    }


}