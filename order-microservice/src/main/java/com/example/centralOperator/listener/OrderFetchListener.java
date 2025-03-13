package com.example.centralOperator.listener;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.service.FetchOrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderFetchListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderFetchListener.class);

    @Autowired
    private FetchOrderService fetchOrderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_FETCH_REQUEST_QUEUE)
    public void listenOrderFetch(Message message) {
        try {
            MessageProperties props = message.getMessageProperties();
            String correlationId = props.getCorrelationId();
            if (correlationId == null || correlationId.isEmpty()) {
                logger.warn("**Received message without correlation ID");
                return;
            }

            String jsonString = new String(message.getBody(), StandardCharsets.UTF_8);
//            logger.info("Received fetch request: " + jsonString);

            String res = fetchOrderService.handleFetchOrders(jsonString);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_FETCH_RESPONSE_QUEUE,
                    res,
                    messagePostProcessor -> {
                        messagePostProcessor.getMessageProperties().setCorrelationId(correlationId);
                        messagePostProcessor.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return messagePostProcessor;
                    }
            );

//            logger.info("**Response sent json: {}", res);
            logger.info("**Response sent with correlation ID: {}", correlationId);

        } catch (Exception e) {
            logger.error("Failed to process order fetch request", e);
        }
    }
}
