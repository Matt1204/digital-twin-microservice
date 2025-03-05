package org.example.taxi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TaxiAgent {
    private TaxiMsgClient taxiClient = new TaxiMsgClient();

    public TaxiAgent() {
    }

    public void invokeTaxiAction(TaxiState taxiState){
//        TaxiMsgClient taxiClient = new TaxiMsgClient();
        String responseJson = taxiClient.publishActionReq(taxiState);
        System.out.println("!!!! Received response: " + responseJson);
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String parsedJson = objectMapper.readValue(responseJson, String.class);
            // parsing json object string to TaxiAction object
            TaxiAction action = objectMapper.readValue(parsedJson, TaxiAction.class);
            System.out.println("Received response: " + action);

        } catch (JsonProcessingException e) {
            System.err.println("!! Failed to parse JSON: " + responseJson);
            e.printStackTrace();
        }


    }
}
