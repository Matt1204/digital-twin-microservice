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
    public static final String ORDER_INIT_REQUEST_QUEUE = "order.init.requests";
    public static final String ORDER_INIT_RESPONSE_QUEUE = "order.init.responses";

    public static final String ORDER_EXCHANGE = "order.exchange";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue orderInitRequestQueue() {
        return new Queue(ORDER_INIT_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue orderInitResponseQueue() {
        return new Queue(ORDER_INIT_RESPONSE_QUEUE, true);
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding orderInitRequestBinding(Queue orderInitRequestQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderInitRequestQueue).to(orderExchange).with(ORDER_INIT_REQUEST_QUEUE);
    }

    @Bean
    public Binding orderInitResponseBinding(Queue orderInitResponseQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderInitResponseQueue).to(orderExchange).with(ORDER_INIT_RESPONSE_QUEUE);
    }
}

