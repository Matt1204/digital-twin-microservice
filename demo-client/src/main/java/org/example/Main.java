package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.centralOperator.CoMsgClient;
import org.example.order.OrderMsgClient;
import org.example.order.TaxiOrder;
import org.example.taxi.TaxiOperationManager;
import org.example.taxi.TaxiOperationType;
import org.example.taxi.TaxiState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        RabbitMQSetup.initialize();

        // 1. Order Microservice
//        OrderMsgClient orderMsgClient = new OrderMsgClient();
//        orderMsgClient.publishInitReq(); // init
//
//        Date currentDate = new Date();
//        System.out.println(currentDate);
//        String fetchedOrdersStr = orderMsgClient.publishFetchOrderReq(new Date(), 300); // fetch order
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        objectMapper.setDateFormat(sdf); // Ensure correct date parsing
//        List<TaxiOrder> orders = objectMapper.readValue(fetchedOrdersStr, new TypeReference<List<TaxiOrder>>() {
//        });


        // 2. Taxi Microservice
//        TaxiAgent taxi = new TaxiAgent();
//        taxi.invokeTaxiAction(new TaxiState(1, 260, 100, -73.57602765306774, 45.49422596892001)); // get taxi action


        // 3. CO Microservice
        CoMsgClient coClient = new CoMsgClient();

        coClient.publishOpDone("0", TaxiOperationType.IDLING);

        // 3.1 updating active Taxis
        coClient.publishTaxiUpdate(new TaxiState("0", -73.575678, 45.497234, 80, 10, TaxiOperationType.REPOSITIONING));
        coClient.publishTaxiUpdate(new TaxiState("1", -73.576890, 45.498567, 100, 50, TaxiOperationType.SERVICE));
        coClient.publishTaxiUpdate(new TaxiState("2", -73.578123, 45.499890, 60, 20, TaxiOperationType.CHARGING));
        coClient.publishTaxiUpdate(new TaxiState("3", -73.579456, 45.501123, 120, 5, TaxiOperationType.OTHER));
//         3.2 updating active orders
        List<TaxiOrder> orderList1 = new ArrayList<>(List.of(
                new TaxiOrder("001", -73.5673, 45.5017, -73.5615, 45.5086, new Date(), 2.3, 12.5),
                new TaxiOrder("004", -73.5689, 45.5025, -73.5642, 45.5098, new Date(), 3.1, 15.0),
                new TaxiOrder("003", -73.5702, 45.5039, -73.5655, 45.5112, new Date(), 1.8, 10.0)
        ));
        List<TaxiOrder> orderList2 = new ArrayList<>(List.of(
                new TaxiOrder("002", -73.5721, 45.5052, -73.5674, 45.5135, new Date(), 4.2, 20.0),
                new TaxiOrder("005", -73.5735, 45.5068, -73.5689, 45.5149, new Date(), 2.7, 14.5)
        ));
        coClient.publishOrdersUpdate(orderList2);
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