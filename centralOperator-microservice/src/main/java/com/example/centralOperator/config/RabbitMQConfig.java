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
    public static final String CO_EXCHANGE = "co.exchange";

    public static final String CO_REQUEST_QUEUE = "co.requests";
    public static final String CO_RESPONSE_QUEUE = "co.responses";

    public static final String CO_MATCH_REQUEST_QUEUE = "co.match.requests";
    public static final String CO_MATCH_RESPONSE_QUEUE = "co.match.responses";
    public static final String CO_ACTIVE_TAXIS_UPDATE_REQUEST_QUEUE = "co.activeTaxis.update.requests";
    public static final String CO_ACTIVE_TAXIS_UPDATE_RESPONSE_QUEUE = "co.activeTaxis.update.responses";
    public static final String CO_ACTIVE_ORDERS_UPDATE_REQUEST_QUEUE = "co.activeOrders.update.requests";
    public static final String CO_ACTIVE_ORDERS_UPDATE_RESPONSE_QUEUE = "co.activeOrders.update.responses";


    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    @Bean
    public Queue coRequestQueue() {
        return new Queue(CO_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue coResponseQueue() {
        return new Queue(CO_RESPONSE_QUEUE, true);
    }

    @Bean
    public Binding coRequestBinding(Queue coRequestQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(coRequestQueue).to(coExchange).with(CO_REQUEST_QUEUE);
    }

    @Bean
    public Binding coResponseBinding(Queue coResponseQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(coResponseQueue).to(coExchange).with(CO_RESPONSE_QUEUE);
    }

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
    public Queue activeTaxisUpdateRequestQueue() {
        return new Queue(CO_ACTIVE_TAXIS_UPDATE_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue activeTaxisUpdateResponseQueue() {
        return new Queue(CO_ACTIVE_TAXIS_UPDATE_RESPONSE_QUEUE, true);
    }
    @Bean
    public Binding taxiUpdateRequestBinding(Queue activeTaxisUpdateRequestQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(activeTaxisUpdateRequestQueue).to(coExchange).with(CO_ACTIVE_TAXIS_UPDATE_REQUEST_QUEUE);
    }
    @Bean
    public Binding taxiUpdateResponseBinding(Queue activeTaxisUpdateResponseQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(activeTaxisUpdateResponseQueue).to(coExchange).with(CO_ACTIVE_TAXIS_UPDATE_RESPONSE_QUEUE);
    }

    @Bean
    public Queue activeOrdersUpdateRequestQueue() {
        return new Queue(CO_ACTIVE_ORDERS_UPDATE_REQUEST_QUEUE, true);
    }
    @Bean
    public Queue activeOrdersUpdateResponseQueue() {
        return new Queue(CO_ACTIVE_ORDERS_UPDATE_RESPONSE_QUEUE, true);
    }

    @Bean
    public Binding orderUpdateRequestBinding(Queue activeOrdersUpdateRequestQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(activeOrdersUpdateRequestQueue).to(coExchange).with(CO_ACTIVE_ORDERS_UPDATE_REQUEST_QUEUE);
    }
    @Bean
    public Binding orderUpdateResponseBinding(Queue activeOrdersUpdateResponseQueue, DirectExchange coExchange) {
        return BindingBuilder.bind(activeOrdersUpdateResponseQueue).to(coExchange).with(CO_ACTIVE_ORDERS_UPDATE_RESPONSE_QUEUE);
    }

}
