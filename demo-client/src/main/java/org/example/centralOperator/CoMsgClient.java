package org.example.centralOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.example.RabbitMQConfig;
import org.example.RabbitMQSetup;
import org.example.taxi.TaxiState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class CoMsgClient {
    private final Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();

    public CoMsgClient() {
        try {
            this.channel = RabbitMQSetup.getChannel();
            listenMatchResponse();
        } catch (Exception e) {
            throw new RuntimeException("Error setting up TaxiRpcClient: " + e.getMessage(), e);
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

    public String publishMatchReq(Map<String, Deque<Integer>> activeOrdersMap, Map<String, Set<Integer>> activeTaxisMap) {
        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> combinedMap = new HashMap<>();
        combinedMap.put("activeOrders", activeOrdersMap);
        combinedMap.put("activeTaxis", activeTaxisMap);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonMaps;

        try {
            jsonMaps = objectMapper.writeValueAsString(combinedMap);
            System.out.println("Serialized JSON: " + jsonMaps);
        } catch (IOException e) {
            System.err.println("Error serializing CO maps: " + e.getMessage());
            return "Error: Failed to serialize message.";
        }

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseMap.put(correlationId, responseFuture);

        try {
            publishMessage(correlationId, jsonMaps);
        } catch (IOException e) {
            System.err.println("Error publishing message to RabbitMQ: " + e.getMessage());
            responseMap.remove(correlationId);
            return "Error: Failed to publish message.";
        }

        try {
            // block, wait for response
            String responseJson = responseFuture.get(5, TimeUnit.SECONDS);
            System.out.println("response JSON: " + responseJson);
            return responseJson;

        } catch (TimeoutException e) {
            System.err.println("Error: Response timeout for correlationId: " + correlationId);
            responseMap.remove(correlationId);
            return "Error: Response timeout.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: Thread was interrupted.");
            return "Error: Thread interrupted.";
        } catch (ExecutionException e) {
            System.err.println("Error processing message response: " + e.getCause().getMessage());
            return "Error: Failed to process response.";
        }
    }

    private void publishMessage(String correlationId, String messageJson) throws IOException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .contentType("application/json")
                .deliveryMode(2) // Persistent message
                .build();

        channel.basicPublish(
                RabbitMQConfig.CO_EXCHANGE,
                RabbitMQConfig.CO_MATCH_REQUEST_ROUTING_KEY,
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
