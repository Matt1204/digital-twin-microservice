package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.model.TaxiState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UtilityMonitorService {

    @Autowired
    TaxiStateMap taxiStateMap;

    @Autowired
    ActiveOrders activeOrders;


    public void calculateUtility(String taxiId, String orderId) {
        TaxiState taxiState = taxiStateMap.findTaxiById(taxiId);
        TaxiOrder order = activeOrders.findOrderById(orderId);
//        System.out.println("monitorUtility: " + taxiState + " --- " + order);

        double taxiUtil = calculateVehicleUtility(taxiState, order);
        double riderUtil = calculateRiderUtility(taxiState, order);
        double totalUtil = taxiUtil + riderUtil;

        System.out.println(String.format("totalUtil = taxi + rider = %f + %f = %f", taxiUtil, riderUtil, totalUtil));
    }

    private double calculateVehicleUtility(TaxiState taxiState, TaxiOrder order) {
        double tripIncome = order.getTripIncome();
        double tripDistance = order.getTripDistance();
        double costPerMile = 0.5;
        double initLongitude = taxiState.getLongitude();
        double initLatitude = taxiState.getLatitude();
        double targetLongitude = order.getPickupLon();
        double targetLatitude = order.getPickupLat();
        double pickupDistance = haversineDistanceInMiles(initLatitude, initLongitude, targetLatitude, targetLongitude);

        double vehicleUtil = tripIncome - costPerMile * (pickupDistance + tripDistance);
        return vehicleUtil;
    }

    private double haversineDistanceInMiles(double lat1, double lon1, double lat2, double lon2) {
        final double R = 3958.8; // Earth's radius in miles
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private double calculateRiderUtility(TaxiState taxiState, TaxiOrder order) {
        double initLongitude = taxiState.getLongitude();
        double initLatitude = taxiState.getLatitude();
        double targetLongitude = order.getPickupLon();
        double targetLatitude = order.getPickupLat();
        double pickupDistance = haversineDistanceInMiles(initLatitude, initLongitude, targetLatitude, targetLongitude);

        double qualityCoEff = 10;
        double costPerMile = 0.5;
        double delayTolRate = 0.2;
        double vehicleSpeed = 35;
        double waitTime = pickupDistance / vehicleSpeed;


        double riderUtil = qualityCoEff * costPerMile - delayTolRate * waitTime;
        return riderUtil;
    }
}
