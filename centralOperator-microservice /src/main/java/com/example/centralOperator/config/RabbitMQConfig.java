package com.example.centralOperator.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String CO_MATCH_REQUEST_QUEUE = "co.match.requests";
    public static final String CO_MATCH_RESPONSE_QUEUE = "co.match.responses";
    public static final String CO_EXCHANGE = "co.exchange";
    public static final String CO_ACTIVE_TAXIS_UPDATE_QUEUE = "co.activeTaxis.update";
    public static final String CO_ACTIVE_ORDERS_UPDATE_QUEUE = "co.activeOrders.update";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Declare the request queue where Taxi clients publish their status.
     */
    @Bean
    public Queue matchRequestQueue() {
        // durable = true to persist messages to disk
        return new Queue(CO_MATCH_REQUEST_QUEUE, true);
    }

    /**
     * Declare the response queue where the microservice publishes processed results.
     */
    @Bean
    public Queue matchResponseQueue() {
        return new Queue(CO_MATCH_RESPONSE_QUEUE, true);
    }

    /**
     * A Direct Exchange to route messages.
     */
    @Bean
    public DirectExchange coExchange() {
        return new DirectExchange(CO_EXCHANGE);
    }

    /**
     * bind QUEUE to EXCHANGE with BINDING_KEY
     */
    @Bean
    public Binding matchRequestBinding(Queue matchRequestQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(matchRequestQueue).to(coExchange).with(CO_MATCH_REQUEST_QUEUE);
    }

    /**
     * Binding for the response queue to the exchange with routing key 'taxi.responses'.
     */
    @Bean
    public Binding matchResponseBinding(Queue matchResponseQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(matchResponseQueue).to(coExchange).with(CO_MATCH_RESPONSE_QUEUE);
    }

    @Bean
    public Queue activeTaxisUpdateQueue() {
        return new Queue(CO_ACTIVE_TAXIS_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue activeOrdersUpdateQueue() {
        return new Queue(CO_ACTIVE_ORDERS_UPDATE_QUEUE, true);
    }

    @Bean
    public Binding taxiUpdateBinding(Queue activeTaxisUpdateQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(activeTaxisUpdateQueue).to(coExchange).with(CO_ACTIVE_TAXIS_UPDATE_QUEUE);
    }

    @Bean
    public Binding orderUpdateBinding(Queue activeOrdersUpdateQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(activeOrdersUpdateQueue).to(coExchange).with(CO_ACTIVE_ORDERS_UPDATE_QUEUE);
    }
}

