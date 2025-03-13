package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.centralOperator.CoMsgClient;
import org.example.order.OrderMsgClient;
import org.example.order.TaxiOrder;
import org.example.taxi.TaxiAgent;
import org.example.taxi.TaxiState;

import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        RabbitMQSetup.initialize();

        OrderMsgClient orderMsgClient = new OrderMsgClient();
        orderMsgClient.publishInitReq();
        Date currentDate = new Date();
        System.out.println(currentDate);
        String fetchedOrdersStr = orderMsgClient.publishFetchOrderReq(new Date(), 300);

//        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(sdf); // Ensure correct date parsing
        List<TaxiOrder> orders = objectMapper.readValue(fetchedOrdersStr, new TypeReference<List<TaxiOrder>>() {});


//        TaxiAgent taxi = new TaxiAgent();
//        taxi.invokeTaxiAction(new TaxiState(1, 260, 100, -73.57602765306774, 45.49422596892001));

        // CO Microservice
        CoMsgClient coClient = new CoMsgClient();

//        // updating active taxis
//        coClient.publishTaxiUpdate(new TaxiState("001", 15, 270, 90, -73.574123456789, 45.496789123456), true);
//        coClient.publishTaxiUpdate(new TaxiState("002", 20, 280, 85, -73.575678901234, 45.497890234567), true);
//        coClient.publishTaxiUpdate(new TaxiState("003", 25, 290, 80, -73.576234567890, 45.498901345678), true);

        // updating active orders
//        List<TaxiOrder> orderList1 = new ArrayList<>(List.of(
//                new TaxiOrder("001", -73.5673, 45.5017, -73.5615, 45.5086, new Date(), 2.3, 12.5),
//                new TaxiOrder("004", -73.5689, 45.5025, -73.5642, 45.5098, new Date(), 3.1, 15.0),
//                new TaxiOrder("003", -73.5702, 45.5039, -73.5655, 45.5112, new Date(), 1.8, 10.0)
//        ));
//        List<TaxiOrder> orderList2 = new ArrayList<>(List.of(
//                new TaxiOrder("002", -73.5721, 45.5052, -73.5674, 45.5135, new Date(), 4.2, 20.0),
//                new TaxiOrder("005", -73.5735, 45.5068, -73.5689, 45.5149, new Date(), 2.7, 14.5)
//        ));
//        coClient.publishOrdersUpdate(orderList1);
//
//        Map<String, Deque<Integer>> activeOrdersMap = new HashMap<>();
//        activeOrdersMap.put("1", new ArrayDeque<>(Arrays.asList(1, 2, 3, 4)));
//        activeOrdersMap.put("2", new ArrayDeque<>(Arrays.asList(5, 6)));
//        Map<String, Set<Integer>> activeTaxisMap = new HashMap<>();
//        activeTaxisMap.put("1", new HashSet<>(Arrays.asList(3, 7, 9, 10)));
//        activeTaxisMap.put("2", new HashSet<>(Arrays.asList(1)));
//        activeTaxisMap.put("3", new HashSet<>(Arrays.asList(5, 14)));
//
//        Map<String, List<Integer>> stringifiedJson = coClient.publishMatchReq(activeOrdersMap, activeTaxisMap);




        RabbitMQSetup.close();


    }


}