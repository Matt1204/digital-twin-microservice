package com.example.centralOperator.listener;

import com.example.centralOperator.config.RabbitMQConfig;
import com.example.centralOperator.model.CoReqType;
import com.example.centralOperator.service.TaxiStateMapService;
import com.example.centralOperator.service.MessagePublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode; // Added import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CoListener {

    private static final Logger logger = LoggerFactory.getLogger(CoListener.class);

//    @Autowired
//    private TaxiStateMapService taxiStateMapService;

//    @Autowired
//    private MessagePublisherService messagePublisherService;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.CO_REQUEST_QUEUE)
    public void listenCoRequest(Message message) {
        try {
            MessageProperties props = message.getMessageProperties();
            String correlationId = props.getCorrelationId();
            if (correlationId == null || correlationId.isEmpty()) {
                logger.warn("**Received message without correlation ID, proceeding with processing");
            } else {
                logger.info("** Processing message with correlation ID: " + correlationId);
            }

            String jsonString = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("** Received taxiStateMap message: " + jsonString);

            // Parse JSON and validate structure
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            if (!jsonNode.has("request_type")) {
                logger.warn("** Missing 'request_type' in message");
                return;
            }

            String requestTypeStr = jsonNode.get("request_type").asText();
            try {
                CoReqType requestType = CoReqType.valueOf(requestTypeStr);

                // Handle different request types (for now, just printing)
                switch (requestType) {
                    case TAXI_OP_DONE:
                        logger.info("** Handling TAXI_OP_DONE request");
                        break;
                    default:
                        logger.warn("** Unhandled request type: " + requestType);
                        break;
                }

            } catch (IllegalArgumentException e) {
                logger.warn("** Invalid 'request_type' value: " + requestTypeStr);
            }

        } catch (Exception e) {
            logger.error("Failed to process CO request message", e);
        }
    }
}
