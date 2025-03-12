package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.centralOperator.CoMsgClient;
import org.example.taxi.TaxiAgent;
import org.example.taxi.TaxiState;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        RabbitMQSetup.initialize();

        TaxiAgent taxi = new TaxiAgent();
        taxi.invokeTaxiAction(new TaxiState(1, 260, 100));


        Map<String, Deque<Integer>> activeOrdersMap = new HashMap<>();
        activeOrdersMap.put("1", new ArrayDeque<>(Arrays.asList(1, 2, 3, 4)));
        activeOrdersMap.put("2", new ArrayDeque<>(Arrays.asList(5, 6)));

        Map<String, Set<Integer>> activeTaxisMap = new HashMap<>();
        activeTaxisMap.put("1", new HashSet<>(Arrays.asList(3, 7, 9, 10)));
        activeTaxisMap.put("2", new HashSet<>(Arrays.asList(1)));
        activeTaxisMap.put("3", new HashSet<>(Arrays.asList(5, 14)));

        CoMsgClient coClient = new CoMsgClient();

        Map<String, List<Integer>> stringifiedJson = coClient.publishMatchReq(activeOrdersMap, activeTaxisMap);
//        try {
//            String resJson = objectMapper.readValue(stringifiedJson, String.class);
//            System.out.println("parsed Res: " + resJson);
//
//            Map<String, List<Integer>> resultMap = objectMapper.readValue(resJson, new TypeReference<Map<String, List<Integer>>>() {});
//            List<Integer> matchedOrders = resultMap.get("matchedOrders");
//            List<Integer> matchedTaxis = resultMap.get("matchedTaxis");
//
//            System.out.println("done");
//
//        }
//        catch (JsonProcessingException e) {
//            System.err.println("!! Failed to parse JSON: " + e);
//            e.printStackTrace();
//        }


        RabbitMQSetup.close();


    }


}