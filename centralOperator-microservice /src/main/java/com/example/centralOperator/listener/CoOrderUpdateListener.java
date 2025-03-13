package com.example.centralOperator.listener;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.service.ActiveOrdersService;
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

@Component
public class CoOrderUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(CoOrderUpdateListener.class);

    @Autowired
    private ActiveOrdersService activeOrdersService;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.CO_ACTIVE_ORDERS_UPDATE_REQUEST_QUEUE)
    public void listenOrderUpdate(Message message) {
        try {
            MessageProperties props = message.getMessageProperties();
            String correlationId = props.getCorrelationId();
            if (correlationId == null || correlationId.isEmpty()) {
                logger.warn("**Received message without correlation ID");
                return;
            }

            String jsonString = new String(message.getBody(), StandardCharsets.UTF_8);
            System.out.println("** activeOrder Listener Received: " + jsonString);

            activeOrdersService.handleAddActiveOrders(jsonString);

            String resJson = objectMapper.writeValueAsString("ORDER_UPDATE_DONE");
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CO_EXCHANGE, // exchange
                    RabbitMQConfig.CO_ACTIVE_ORDERS_UPDATE_RESPONSE_QUEUE, // routing_key
                    resJson,
                    messagePostProcessor -> {
                        messagePostProcessor.getMessageProperties().setCorrelationId(correlationId);
                        messagePostProcessor.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return messagePostProcessor;
                    }
            );
            System.out.println("** activeOrder response: " + resJson);

        } catch (Exception e) {
            logger.error("Failed to process order update message", e);
        }
    }
}
