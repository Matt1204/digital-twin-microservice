package org.example.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class OrderMsgClient {
    private final Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();

    public OrderMsgClient() {
        try {
            this.channel = RabbitMQSetup.getChannel();
            listenInitResponse();
            listenFetchResponse();
        } catch (Exception e) {
            throw new RuntimeException("Error setting up CoMsgClient: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a single consumer for the response queue.
     */
    private void listenInitResponse() throws IOException {
        channel.basicConsume(RabbitMQConfig.ORDER_INIT_RESPONSE_QUEUE, false,
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

    private void listenFetchResponse() throws IOException {
        channel.basicConsume(RabbitMQConfig.ORDER_FETCH_RESPONSE_QUEUE, false,
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

    public void publishInitReq() {
        String correlationId = UUID.randomUUID().toString();

        try {
            String messageJson = objectMapper.writeValueAsString("initialize");
            publishCorrelationMessage(correlationId, messageJson, RabbitMQConfig.ORDER_INIT_REQUEST_ROUTING_KEY);
        } catch (IOException e) {
            System.err.println("Error publishing order init: " + e.getMessage());
        }

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseMap.put(correlationId, responseFuture);


        try {
            // block, wait for response
            String stringifiedJson = responseFuture.get(5, TimeUnit.SECONDS);
            System.out.println("raw JSON: " + stringifiedJson);
            String parsedJson = objectMapper.readValue(stringifiedJson, String.class);
            System.out.println("parsed json: " + parsedJson);

        } catch (JsonMappingException e) {
            System.err.println("JSON mapping error: " + e.getMessage());
        } catch (JsonProcessingException e) {
            System.err.println("JSON processing error: " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("Execution error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread was interrupted: " + e.getMessage());
        } catch (TimeoutException e) {
            System.err.println("Request timed out: " + e.getMessage());
        }
    }

    public String publishFetchOrderReq(Date curDatetime, int timeWindow) {
        String correlationId = UUID.randomUUID().toString();

        try {
            Map<String, Object> reqMap = new HashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            reqMap.put("curDatetime", sdf.format(curDatetime));
            reqMap.put("timeWindow", timeWindow);
            String messageJson = objectMapper.writeValueAsString(reqMap);
            publishCorrelationMessage(correlationId, messageJson, RabbitMQConfig.ORDER_FETCH_REQUEST_ROUTING_KEY);
        } catch (IOException e) {
            System.err.println("Error publishing order fetch: " + e.getMessage());
        }

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseMap.put(correlationId, responseFuture);

        try {
            // block, wait for response
            String stringifiedJson = responseFuture.get(5, TimeUnit.SECONDS);
//            System.out.println("raw JSON: " + stringifiedJson);
            String jsonString = objectMapper.readValue(stringifiedJson, String.class);
            System.out.println("parsed json: " + jsonString);
           return  jsonString;

        } catch (JsonMappingException e) {
            System.err.println("JSON mapping error: " + e.getMessage());
            return "[]";
        } catch (JsonProcessingException e) {
            System.err.println("JSON processing error: " + e.getMessage());
            return "[]";
        } catch (ExecutionException e) {
            System.err.println("Execution error: " + e.getMessage());
            return "[]";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread was interrupted: " + e.getMessage());
            return "[]";
        } catch (TimeoutException e) {
            System.err.println("Request timed out: " + e.getMessage());
            return "[]";
        }
    }

    private void publishMessage(String messageJson, String routingKey) throws IOException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2) // Persistent message
                .build();

        channel.basicPublish(
                RabbitMQConfig.ORDER_EXCHANGE,
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
                RabbitMQConfig.ORDER_EXCHANGE,
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

