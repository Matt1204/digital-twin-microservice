package org.example;

public class RabbitMQConfig {
    public static final String TAXI_REQUEST_ROUTING_KEY = "taxi.requests";
    public static final String TAXI_RESPONSE_QUEUE = "taxi.responses";
    public static final String TAXI_EXCHANGE = "taxi.exchange";

    public static final String CO_MATCH_REQUEST_ROUTING_KEY = "co.match.requests";
    public static final String CO_MATCH_RESPONSE_QUEUE = "co.match.responses";
    public static final String CO_EXCHANGE = "co.exchange";
}

