package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQSetup {

    private static Connection connection;
    private static Channel channel;

    /**
     * Initializes the RabbitMQ connection, channel, queue, and exchange.
     */
    public static void initialize() {
        try{
            if (connection == null || !connection.isOpen()) {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("localhost");
                factory.setUsername("admin");
                factory.setPassword("admin");
                factory.setPort(5672);
                connection = factory.newConnection();
            }

            if (channel == null || !channel.isOpen()) {
                channel = connection.createChannel();

//                channel.exchangeDeclare(RabbitMQConfig.TAXI_EXCHANGE, "direct", true);
//                channel.queueDeclare(RabbitMQConfig.TAXI_REQUEST_QUEUE, true, false, false, null);
//                channel.queueDeclare(RabbitMQConfig.TAXI_RESPONSE_QUEUE, true, false, false, null);
//
//                channel.queueBind(RabbitMQConfig.TAXI_REQUEST_QUEUE, RabbitMQConfig.TAXI_EXCHANGE, RabbitMQConfig.TAXI_REQUEST_QUEUE);
//                channel.queueBind(RabbitMQConfig.TAXI_RESPONSE_QUEUE, RabbitMQConfig.TAXI_EXCHANGE, RabbitMQConfig.TAXI_RESPONSE_QUEUE);
            }
        } catch (Exception e) {
            System.out.println("Error initializing rabbitMQ connection: " + e);
        }
    }

    /**
     * Returns the RabbitMQ connection.
     */
    public static Connection getConnection() throws Exception {
        if (connection == null || !connection.isOpen()) {
            initialize();
        }
        return connection;
    }

    /**
     * Returns the RabbitMQ channel.
     */
    public static Channel getChannel() throws Exception {
        if (channel == null || !channel.isOpen()) {
            initialize();
        }
        return channel;
    }

    /**
     * Closes the RabbitMQ connection and channel.
     */
    public static void close() throws Exception {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}