package com.example.demo_microservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TAXI_REQUEST_QUEUE = "taxi.requests";
    public static final String TAXI_RESPONSE_QUEUE = "taxi.responses";
    public static final String TAXI_EXCHANGE = "taxi.exchange";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Declare the request queue where Taxi clients publish their status.
     */
    @Bean
    public Queue requestQueue() {
        // durable = true to persist messages to disk
        return new Queue(TAXI_REQUEST_QUEUE, true);
    }

    /**
     * Declare the response queue where the microservice publishes processed results.
     */
    @Bean
    public Queue responseQueue() {
        return new Queue(TAXI_RESPONSE_QUEUE, true);
    }

    /**
     * A Direct Exchange to route messages.
     */
    @Bean
    public DirectExchange taxiExchange() {
        return new DirectExchange(TAXI_EXCHANGE);
    }

    /**
     * bind QUEUE to EXCHANGE with BINDING_KEY
     */
    @Bean
    public Binding requestBinding(Queue requestQueue, DirectExchange taxiExchange) {
        return BindingBuilder.bind(requestQueue).to(taxiExchange).with(TAXI_REQUEST_QUEUE);
    }

    /**
     * Binding for the response queue to the exchange with routing key 'taxi.responses'.
     */
    @Bean
    public Binding responseBinding(Queue responseQueue, DirectExchange taxiExchange) {
        return BindingBuilder.bind(responseQueue).to(taxiExchange).with(TAXI_RESPONSE_QUEUE);
    }
}

