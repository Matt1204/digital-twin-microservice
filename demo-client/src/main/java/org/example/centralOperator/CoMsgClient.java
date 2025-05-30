package org.example.centralOperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.example.CoReqType;
import org.example.CoResType;
import org.example.RabbitMQConfig;
import org.example.RabbitMQSetup;
import org.example.order.TaxiOrder;
import org.example.taxi.TaxiOperationType;
import org.example.taxi.TaxiState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class CoMsgClient {
    private final Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();

    public CoMsgClient() {
        try {
            this.channel = RabbitMQSetup.getChannel();

            listenCoMsg();
            listenMatchResponse();
            listenUpdateTaxiResponse();
            listenUpdateOrderResponse();
        } catch (Exception e) {
            throw new RuntimeException("Error setting up CoMsgClient: " + e.getMessage(), e);
        }
    }

    public void listenCoMsg() {
        try {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String correlationId = delivery.getProperties().getCorrelationId();

                String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String parsedMsg = objectMapper.readValue(messageBody, String.class);
                System.out.println(" [x] Received raw message: " + parsedMsg);

                try {
                    // Parse the message as JSON
                    JsonNode jsonNode = objectMapper.readTree(parsedMsg);

                    // Ensure the "responseType" field exists
                    if (!jsonNode.has("responseType")) {
                        System.err.println(" [!] Missing 'responseType' field in message!");
                        return;
                    }

                    try {
                        String responseTypeStr = jsonNode.get("responseType").asText();
                        CoResType responseType = CoResType.valueOf(responseTypeStr);

                        if (correlationId == null || correlationId.isEmpty()) {
                            handleResponse(responseType, jsonNode);
                        } else {
                            handleCorrelationResponse(responseType, parsedMsg, correlationId);
                        }

                    } catch (IllegalArgumentException e) {
                        System.err.println(" [!] invalid 'responseType': " + e.getMessage());
                    }

                } catch (Exception e) {
                    System.err.println(" [!] Failed to process message: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            // Start consuming messages
            System.out.println(" [x] listening co.responses... ");
            channel.basicConsume(RabbitMQConfig.CO_RESPONSE_QUEUE, true, deliverCallback, consumerTag -> {
            });

        } catch (IOException e) {
            System.err.println(" [!] Error connecting to RabbitMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process the message based on the responseType.
     */
    private void handleResponse(CoResType responseType, JsonNode jsonNode) {
        switch (responseType) {
            case NEW_TAXI_OPERATION:
                System.out.println(" [✓] Processing NEW_TAXI_OPERATION response");
                break;
            default:
                System.out.println(" [!] Unknown response type: " + responseType);
                break;
        }
    }

    private void handleCorrelationResponse(CoResType responseType, String parsedJson, String correlationId) {
        CompletableFuture<String> future = responseMap.remove(correlationId);
        if (future != null) {
            System.out.println("HIT Future fulfilled");
            future.complete(parsedJson);
        } else {
            System.err.println("Future not found for correlationId: " + correlationId);
        }
    }


    /**
     * Registers a single consumer for the response queue.
     */
    private void listenMatchResponse() throws IOException {
        channel.basicConsume(RabbitMQConfig.CO_MATCH_RESPONSE_QUEUE, false,
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String correlationId = properties.getCorrelationId();
                        String responseJson = new String(body, StandardCharsets.UTF_8);
                        channel.basicAck(envelope.getDeliveryTag(), false);
                        System.out.println("Raw message from RabbitMQ: " + responseJson);


                        if (correlationId != null) {
                            System.out.println("HIT correlationID ");
                            CompletableFuture<String> future = responseMap.remove(correlationId);
                            if (future != null) {
                                System.out.println("HIT Future fulfilled");
                                future.complete(responseJson);
                            }
                        }
                    }
                }
        );
    }

    private void listenUpdateTaxiResponse() throws IOException {
        channel.basicConsume(RabbitMQConfig.CO_ACTIVE_TAXIS_UPDATE_RESPONSE_QUEUE, false,
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String correlationId = properties.getCorrelationId();
                        String responseJson = new String(body, StandardCharsets.UTF_8);
                        channel.basicAck(envelope.getDeliveryTag(), false);

                        if (correlationId != null) {
                            CompletableFuture<String> future = responseMap.remove(correlationId);
                            if (future != null) {
                                future.complete(responseJson);
                            }
                        }
                    }
                }
        );
    }

    private void listenUpdateOrderResponse() throws IOException {
        channel.basicConsume(RabbitMQConfig.CO_ACTIVE_ORDERS_UPDATE_RESPONSE_QUEUE, false,
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String correlationId = properties.getCorrelationId();
                        String responseJson = new String(body, StandardCharsets.UTF_8);
                        channel.basicAck(envelope.getDeliveryTag(), false);

                        if (correlationId != null) {
                            CompletableFuture<String> future = responseMap.remove(correlationId);
                            if (future != null) {
                                future.complete(responseJson);
                            }
                        }
                    }
                }
        );
    }

    public void publishBMDDPGWindowStarts() {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("requestType", CoReqType.BMDDPG_WINDOW_STARTS);

        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            this.publishMessage(msgJson, RabbitMQConfig.CO_REQUEST_ROUTING_KEY);

        } catch (JsonProcessingException e) {
            System.err.println("Error serializing messageData: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error publishing publishOpDone: " + e.getMessage());
        }
    }

    public void publishCoInit() {
        try {
            String correlationId = UUID.randomUUID().toString();
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("requestType", CoReqType.CO_INIT);
            try {
                String jsonMsg = objectMapper.writeValueAsString(messageData);
                this.publishCorrelationMessage(correlationId, jsonMsg, RabbitMQConfig.CO_REQUEST_ROUTING_KEY);
            } catch (JsonProcessingException e) {
                System.err.println("Error publishing CON_INIT request: " + e.getMessage());
                return;
            }

            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            responseMap.put(correlationId, responseFuture);

            try {
                // block, wait for response
                String stringifiedJson = responseFuture.get(5, TimeUnit.SECONDS);
                System.out.println("[x] CO_INIT response: " + stringifiedJson);
                String parsedJson = objectMapper.readValue(stringifiedJson, String.class);

            } catch (JsonMappingException e) {
                System.err.println("error receiving CO_INIT response: " + e);
            } catch (JsonProcessingException e) {
                System.err.println("error receiving CO_INIT response: " + e);
            } catch (ExecutionException e) {
                System.err.println("error receiving CO_INIT response: " + e);
            } catch (InterruptedException e) {
                System.err.println("error receiving CO_INIT response: " + e);
            } catch (TimeoutException e) {
                System.err.println("error receiving CO_INIT response: " + e);
            }


        } catch (IOException e) {
            System.err.println("Error publishing CO init: " + e.getMessage());
        }
    }

    public void publishTaxiUpdate(TaxiState taxiState) {
        try {
            String correlationId = UUID.randomUUID().toString();

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("taxiState", taxiState);
            String jsonTaxiState = objectMapper.writeValueAsString(messageData);

            try {
                publishCorrelationMessage(correlationId, jsonTaxiState, RabbitMQConfig.CO_ACTIVE_TAXIS_UPDATE_REQUEST_ROUTING_KEY);
            } catch (IOException e) {
                System.err.println("Error publishing CO activeTaxi: " + e.getMessage());
            }

            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            responseMap.put(correlationId, responseFuture);


            try {
                // block, wait for response
                String stringifiedJson = responseFuture.get(5, TimeUnit.SECONDS);
                System.out.println("CO updateTaxi response: " + stringifiedJson);
                String parsedJson = objectMapper.readValue(stringifiedJson, String.class);

            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            System.err.println("Error publishing TaxiState update: " + e.getMessage());
        }
    }

    public void publishOrdersUpdate(List<TaxiOrder> orderList) {
        try {
            String correlationId = UUID.randomUUID().toString();

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("orderList", orderList);

            String jsonOrderList = objectMapper.writeValueAsString(messageData);

            try {
                publishCorrelationMessage(correlationId, jsonOrderList, RabbitMQConfig.CO_ACTIVE_ORDERS_UPDATE_REQUEST_ROUTING_KEY);
            } catch (IOException e) {
                System.err.println("Error publishing CO activeOrder: " + e.getMessage());
            }

            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            responseMap.put(correlationId, responseFuture);
            try {
                // block, wait for response
                String stringifiedJson = responseFuture.get(5, TimeUnit.SECONDS);
                System.out.println("CO updateOrder response: " + stringifiedJson);
                String parsedJson = objectMapper.readValue(stringifiedJson, String.class);

            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            System.err.println("Error publishing TaxiState update: " + e.getMessage());
        }
    }

    public void publishOpDone(String taxiId, TaxiOperationType opType) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("requestType", CoReqType.TAXI_OP_DONE);

        Map<String, Object> payload = new HashMap<>();
        payload.put("taxiId", taxiId);
        payload.put("operationType", opType);
        messageData.put("payload", payload);

        try {
            String msgJson = objectMapper.writeValueAsString(messageData);
            this.publishMessage(msgJson, RabbitMQConfig.CO_REQUEST_ROUTING_KEY);

        } catch (JsonProcessingException e) {
            System.err.println("Error serializing messageData: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error publishing publishOpDone: " + e.getMessage());
        }
    }

    private void publishMessage(String messageJson, String routingKey) throws IOException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2) // Persistent message
                .build();

        channel.basicPublish(
                RabbitMQConfig.CO_EXCHANGE,
                routingKey,
                props,
                messageJson.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void publishCorrelationMessage(String correlationId, String messageJson, String routingKey) throws IOException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .contentType("application/json")
                .deliveryMode(2) // Persistent message
                .build();

        channel.basicPublish(
                RabbitMQConfig.CO_EXCHANGE,
                routingKey,
                props,
                messageJson.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Converts a TaxiStatus object to JSON.
     */
    private String serializeToJson(TaxiState taxiState) throws IOException {
        return objectMapper.writeValueAsString(taxiState);
    }
}
