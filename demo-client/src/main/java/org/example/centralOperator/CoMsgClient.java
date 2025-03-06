package org.example.centralOperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
            throw new RuntimeException("Error setting up CoMsgClient: " + e.getMessage(), e);
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

    public Map<String, List<Integer>> publishMatchReq(Map<String, Deque<Integer>> activeOrdersMap, Map<String, Set<Integer>> activeTaxisMap) {
        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> combinedMap = new HashMap<>();
        combinedMap.put("activeOrders", activeOrdersMap);
        combinedMap.put("activeTaxis", activeTaxisMap);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonMaps;

        try {
            jsonMaps = objectMapper.writeValueAsString(combinedMap);
            System.out.println("Serialized JSON: " + jsonMaps);
            publishMessage(correlationId, jsonMaps);
        } catch (IOException e) {
            System.err.println("Error publishing CO maps: " + e.getMessage());
        }

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseMap.put(correlationId, responseFuture);


        try {
            // block, wait for response
            String stringifiedJson = responseFuture.get(5, TimeUnit.SECONDS);
            System.out.println("CO raw JSON: " + stringifiedJson);
            String parsedJson = objectMapper.readValue(stringifiedJson, String.class);
            System.out.println("CO parsed json: " + parsedJson);

            Map<String, List<Integer>> matchingMap = objectMapper.readValue(parsedJson, new TypeReference<Map<String, List<Integer>>>() {});
            return matchingMap;

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
