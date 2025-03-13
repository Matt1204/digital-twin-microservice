package org.example;

public class RabbitMQConfig {
    public static final String TAXI_REQUEST_ROUTING_KEY = "taxi.requests";
    public static final String TAXI_RESPONSE_QUEUE = "taxi.responses";
    public static final String TAXI_EXCHANGE = "taxi.exchange";

    public static final String CO_MATCH_REQUEST_ROUTING_KEY = "co.match.requests";
    public static final String CO_MATCH_RESPONSE_QUEUE = "co.match.responses";
    public static final String CO_ACTIVE_TAXIS_UPDATE_REQUEST_ROUTING_KEY = "co.activeTaxis.update.requests";
    public static final String CO_ACTIVE_TAXIS_UPDATE_RESPONSE_QUEUE = "co.activeTaxis.update.responses";
    public static final String CO_ACTIVE_ORDERS_UPDATE_REQUEST_ROUTING_KEY = "co.activeOrders.update.requests";
    public static final String CO_ACTIVE_ORDERS_UPDATE_RESPONSE_QUEUE = "co.activeOrders.update.responses";
    public static final String CO_EXCHANGE = "co.exchange";

    public static final String ORDER_INIT_REQUEST_ROUTING_KEY = "order.init.requests";
    public static final String ORDER_INIT_RESPONSE_QUEUE = "order.init.responses";
    public static final String ORDER_FETCH_REQUEST_ROUTING_KEY = "order.fetch.requests";
    public static final String ORDER_FETCH_RESPONSE_QUEUE = "order.fetch.responses";
    public static final String ORDER_EXCHANGE = "order.exchange";


}

