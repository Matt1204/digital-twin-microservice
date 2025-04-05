package com.example.centralOperator.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class SimulationTimeService {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date simulationDateTime;

    public void handleTimeUpdate(JsonNode payloadNode){
        String datetimeStr = payloadNode.get("time").asText();
//        System.out.println("new datetime: " + datetimeStr);
        try {
            Date startDatetime = sdf.parse(datetimeStr);
            this.setSimulationDateTime(startDatetime);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Date getSimulationDateTime() {
        return simulationDateTime;
    }

    public void setSimulationDateTime(Date simulationDateTime) {
        this.simulationDateTime = simulationDateTime;
    }

    public String getSimulationTimeStr() {
        if (simulationDateTime == null) return null;
        return new SimpleDateFormat("HH:mm:ss").format(simulationDateTime);
    }

    public String getSimulationDateTimeStr() {
        if (simulationDateTime == null) return null;
        return sdf.format(simulationDateTime);
    }
}
