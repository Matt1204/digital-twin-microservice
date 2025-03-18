package com.example.centralOperator.service;

import com.example.centralOperator.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessagePublisherService {

    private static final Logger logger = LoggerFactory.getLogger(MessagePublisherService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Publishes a message as a JSON string directly to the RabbitMQ exchange.
     *
     * @param routingKey The routing key to send the message to.
     * @param jsonMessage The JSON string to be sent.
     */
    public void publishMessage(String routingKey, String jsonMessage) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.CO_EXCHANGE, routingKey, jsonMessage);
            logger.info("Message published to {}: {}", routingKey, jsonMessage);
        } catch (Exception e) {
            logger.error("Failed to publish message", e);
        }
    }

    /**
     * Publishes a message as a JSON string with a correlation ID attached.
     *
     * @param routingKey The routing key to send the message to.
     * @param jsonMessage The JSON string to be sent.
     * @param correlationId The correlation ID to attach to the message.
     */
    public void publishMessageWithCorrelationId(String routingKey, String jsonMessage, String correlationId) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.CO_EXCHANGE, routingKey, jsonMessage, message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            });
            logger.info("Message published to {}: {} with correlationId: {}", routingKey, jsonMessage, correlationId);
        } catch (Exception e) {
            logger.error("Failed to publish message with correlation ID", e);
        }
    }
}