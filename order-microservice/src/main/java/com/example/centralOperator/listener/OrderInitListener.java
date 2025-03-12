package com.example.centralOperator.listener;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.service.AllOrders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OrderInitListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderInitListener.class);

    @Autowired
    private  AllOrders allOrders;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_INIT_REQUEST_QUEUE)
    public void listenOrderInit(Message message) {
        try {
            MessageProperties props = message.getMessageProperties();
            // Retrieve and validate correlation ID
            String correlationId = props.getCorrelationId();
            if (correlationId == null || correlationId.isEmpty()) {
                logger.warn("**Received message without correlation ID");
                return;
            }

            String jsonString = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("Received init msg: " + jsonString);

            allOrders.loadOrdersFromExcel("2010-06-01+3_trips.xlsx");

            String res = objectMapper.writeValueAsString("initialize done");

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE, // exchange
                    RabbitMQConfig.ORDER_INIT_RESPONSE_QUEUE, // routing_key
                    res,
                    messagePostProcessor -> {
                        messagePostProcessor.getMessageProperties().setCorrelationId(correlationId);
                        messagePostProcessor.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return messagePostProcessor;
                    }
            );
            logger.info("**Response sent json: {}", res);
            logger.info("**Response sent with correlation ID: {}", correlationId);

        } catch (Exception e) {
            logger.error("Failed to process taxi update message", e);
        }
    }
}
