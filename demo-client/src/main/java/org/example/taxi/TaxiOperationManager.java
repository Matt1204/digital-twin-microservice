package org.example.taxi;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

public class TaxiOperationManager implements Serializable {
//    Roadnetwork roadnetwork;
//
//    public TaxiOperationManager(Roadnetwork roadnetwork) {
//        this.roadnetwork = roadnetwork;
//    }
//
//    public void doIdling(String taxiId, TaxiIdlingData idlingData) {
//        Taxi taxiAgent = findFirst(this.roadnetwork.taxis, t -> t.curState.getTaxiId().equals(taxiId));
//        taxiAgent.taxiIdlingData = idlingData;
//        taxiAgent.masterStateChart.receiveMessage("ABORT_OPERATION");
//        taxiAgent.masterStateChart.fireEvent(TaxiOperationType.IDLING);
//    }
//
//    public void doRepositioning(String taxiId, TaxiRepositionData repositioningData) {
//        Taxi taxiAgent = findFirst(this.roadnetwork.taxis, t -> t.curState.getTaxiId().equals(taxiId));
//        taxiAgent.taxiRepositionData = repositioningData;
//        taxiAgent.masterStateChart.receiveMessage("ABORT_OPERATION");
//        taxiAgent.masterStateChart.fireEvent(TaxiOperationType.REPOSITIONING);
//    }
//
//    public void doService(String taxiId, TaxiServiceData taxiServiceData) {
//        Taxi taxiAgent = findFirst(this.roadnetwork.taxis, t -> t.curState.getTaxiId().equals(taxiId));
//        taxiAgent.taxiServiceData = taxiServiceData;
//        taxiAgent.masterStateChart.receiveMessage("ABORT_OPERATION");
//        taxiAgent.masterStateChart.fireEvent(TaxiOperationType.SERVICE);
//    }

    public void handleDoIdling(JsonNode payloadNode) {
        if (payloadNode == null || !payloadNode.has("taxiId") || !payloadNode.has("idleTime")) {
            System.err.println("Error: Missing required fields in handleDoIdling");
            return;
        }

        JsonNode taxiIdNode = payloadNode.get("taxiId");
        JsonNode idleTimeNode = payloadNode.get("idleTime");

        if (!taxiIdNode.isTextual() || !idleTimeNode.isInt()) {
            System.err.println("Error: wrong data type in handleDoIdling");
            return;
        }

        String taxiId = taxiIdNode.asText();
        int idleTime = idleTimeNode.asInt();

    }

    public void handleDoRepositioning(JsonNode payloadNode) {
        if (
                payloadNode == null
                || !payloadNode.has("taxiId")
                || !payloadNode.has("toLat")
                || !payloadNode.has("toLat")
        ) {
            System.err.println("Error: Missing required fields in handleDoRepositioning");
            return;
        }

        JsonNode taxiIdNode = payloadNode.get("taxiId");
        JsonNode idleTimeNode = payloadNode.get("idleTime");
        if (!taxiIdNode.isTextual() || !idleTimeNode.isInt()) {
            System.err.println("Error: wrong data type in handleDoRepositioning");
            return;
        }

        String taxiId = taxiIdNode.asText();
        int idleTime = idleTimeNode.asInt();

    }

}