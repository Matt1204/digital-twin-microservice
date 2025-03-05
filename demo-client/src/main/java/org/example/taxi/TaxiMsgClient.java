package org.example.taxi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.example.RabbitMQConfig;
import org.example.RabbitMQSetup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

public class TaxiMsgClient {
    private final Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();

    public TaxiMsgClient() {
        try {
            this.channel = RabbitMQSetup.getChannel();
            listenActionResponse();
        } catch (Exception e) {
            throw new RuntimeException("Error setting up TaxiRpcClient: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a single consumer for the response queue.
     */
    private void listenActionResponse() throws IOException {
        channel.basicConsume(RabbitMQConfig.TAXI_RESPONSE_QUEUE, false,
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

    public String publishActionReq(TaxiState taxiState) {
        String correlationId = UUID.randomUUID().toString();
        String messageJson;

        try {
            messageJson = serializeToJson(taxiState);
        } catch (IOException e) {
            System.err.println("Error serializing TaxiStatus: " + e.getMessage());
            return "Error: Failed to serialize message.";
        }

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseMap.put(correlationId, responseFuture);

        System.out.println(taxiState.getSoc() + " publish");
        try {
            publishMessage(correlationId, messageJson);
        } catch (IOException e) {
            System.err.println("Error publishing message to RabbitMQ: " + e.getMessage());
            responseMap.remove(correlationId);
            return "Error: Failed to publish message.";
        }

        try {
            String responseJson = responseFuture.get(5, TimeUnit.SECONDS);
            System.out.println("Publisher responseJson: " + responseJson);
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
                RabbitMQConfig.TAXI_EXCHANGE,
                RabbitMQConfig.TAXI_REQUEST_ROUTING_KEY,
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