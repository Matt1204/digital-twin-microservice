package com.example.centralOperator.service;

import com.example.centralOperator.model.TaxiOrder;
import com.example.centralOperator.model.TaxiState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class UtilityMonitorService {

    @Autowired
    TaxiStateMap taxiStateMap;

    @Autowired
    ActiveOrders activeOrders;

    @Autowired
    SimulationTimeService simulationTimeService;

    private static final double COST_PER_MILE = 0.5;
    private static final double QUALITY_COEFF = 10;
    private static final double DELAY_TOL_RATE = 0.2;
    private static final double VEHICLE_SPEED = 35.0;

    private Path currentUtilityFile;

    public void initializeUtilityLogging() {
        Path outputDir = Paths.get("output");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd_HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String filename = timestamp + "_utility.xlsx";
        Path filePath = outputDir.resolve(filename);

        this.currentUtilityFile = filePath;
        createUtilityFile(filePath);
    }

    public void createUtilityFile(Path filePath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
            var sheet = workbook.createSheet("Sheet1");

            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("totalUtility");
            headerRow.createCell(1).setCellValue("vehicleUtility");
            headerRow.createCell(2).setCellValue("riderUtility");
            headerRow.createCell(3).setCellValue("timestamp");

            workbook.write(fileOut);
            System.out.println("XLSX file created: " + filePath.toString());
        } catch (IOException e) {
            System.err.println("Error while creating Excel file: " + e.getMessage());
        }
    }

    public void calculateUtility(String taxiId, String orderId) {
        TaxiState taxiState = taxiStateMap.findTaxiById(taxiId);
        TaxiOrder order = activeOrders.findOrderById(orderId);
//        System.out.println("monitorUtility: " + taxiState + " --- " + order);

        double taxiUtil = calculateVehicleUtility(taxiState, order);
        double riderUtil = calculateRiderUtility(taxiState, order);
        double totalUtil = taxiUtil + riderUtil;

        System.out.println(String.format("totalUtil = taxi + rider = %f + %f = %f", taxiUtil, riderUtil, totalUtil));

        String timestamp = simulationTimeService.getSimulationTimeStr();
        writeUtility(taxiUtil, riderUtil, totalUtil, timestamp);
    }

    private double calculateVehicleUtility(TaxiState taxiState, TaxiOrder order) {
        double tripIncome = order.getTripIncome();
        double tripDistance = order.getTripDistance();
        double initLongitude = taxiState.getLongitude();
        double initLatitude = taxiState.getLatitude();
        double targetLongitude = order.getPickupLon();
        double targetLatitude = order.getPickupLat();
        double pickupDistance = haversineDistanceInMiles(initLatitude, initLongitude, targetLatitude, targetLongitude);
        System.out.println("!!! pickupDistance: " + pickupDistance);
        double vehicleUtil = tripIncome - COST_PER_MILE * (pickupDistance + tripDistance);
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

        double waitTime = pickupDistance / VEHICLE_SPEED;

        double riderUtil = QUALITY_COEFF * COST_PER_MILE - DELAY_TOL_RATE * waitTime;
        return riderUtil;
    }

    private void writeUtility(double taxiUtil, double riderUtil, double totalUtil, String getSimulationTimeStr) {
        if (currentUtilityFile == null) {
            System.err.println("Utility file not initialized.");
            return;
        }

        try (var fis = Files.newInputStream(currentUtilityFile);
             var workbook = new XSSFWorkbook(fis)) {

            var sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            var newRow = sheet.createRow(lastRowNum + 1);

            newRow.createCell(0).setCellValue(totalUtil);
            newRow.createCell(1).setCellValue(taxiUtil);
            newRow.createCell(2).setCellValue(riderUtil);
            newRow.createCell(3).setCellValue(getSimulationTimeStr);

            try (var fos = Files.newOutputStream(currentUtilityFile)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            System.err.println("Error writing utility data: " + e.getMessage());
        }
    }
}
