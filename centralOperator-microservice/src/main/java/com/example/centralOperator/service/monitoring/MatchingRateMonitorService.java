package com.example.centralOperator.service.monitoring;

import com.example.centralOperator.service.ActiveOrders;
import com.example.centralOperator.service.SimulationTimeService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MatchingRateMonitorService {
    @Autowired
    private ActiveOrders activeOrders;

    @Autowired
    private SimulationTimeService simulationTimeService;

    private Path curMatchRateFile;

    private String windowStartTime;
    private int initOrderCount;
    private int addedOrderCount;
    private int matchedOrderCount;


    public void calculateMatchingRate(List<String> matchedOrders) {
        int totalOrder = initOrderCount + addedOrderCount;
        if(totalOrder == 0) return;
        double matchingRate = (double) matchedOrderCount / totalOrder;


        System.out.println(String.format(" -- Matching rate = matched / total_order = %d / %d = %f", matchedOrderCount, totalOrder, matchingRate));

        String timestamp_now = simulationTimeService.getSimulationTimeStr();
        this.writeMatchingRate(matchedOrderCount, totalOrder, matchingRate, windowStartTime, timestamp_now);

        this.initForNewWindow();
    }

    public void incrementAddedOrder(int orderAdded){
        this.addedOrderCount += orderAdded;
    }

    public void incrementMatchedOrder(int orderMatched){
        this.matchedOrderCount += orderMatched;
    }

    private void initForNewWindow(){
        this.initOrderCount = this.activeOrders.getActiveOrdersCount();
        this.addedOrderCount = 0;
        this.matchedOrderCount = 0;
//        this.windowStartTime = this.simulationTimeService.getSimulationDateTimeStr();
    }

    public void initMatchingRateLogging() {
        Path outputDir = Paths.get("output");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String filename = timestamp + "_matching-rate.xlsx";
        Path filePath = outputDir.resolve(filename);

        this.curMatchRateFile = filePath;

        this.initForNewWindow();

        createUtilityFile(filePath);
    }

    public void createUtilityFile(Path filePath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
            var sheet = workbook.createSheet("Sheet1");

            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("matchedRiders");
            headerRow.createCell(1).setCellValue("allRiders");
            headerRow.createCell(2).setCellValue("matchingRate");
            headerRow.createCell(3).setCellValue("timestamp");
//            headerRow.createCell(3).setCellValue("timestamp_end");

            workbook.write(fileOut);
            System.out.println("match-rate.xlsx created: " + filePath.toString());
        } catch (IOException e) {
            System.err.println("Error while creating Excel file: " + e.getMessage());
        }
    }

    private void writeMatchingRate(int matched, int total, double matchingRate, String startTime, String endTime) {
        if (this.curMatchRateFile == null) {
            System.err.println("matching-rate file not initialized.");
            return;
        }

        try (var fis = Files.newInputStream(this.curMatchRateFile);
             var workbook = new XSSFWorkbook(fis)) {

            var sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            var newRow = sheet.createRow(lastRowNum + 1);

            newRow.createCell(0).setCellValue(matched);
            newRow.createCell(1).setCellValue(total);
            newRow.createCell(2).setCellValue(matchingRate);
            newRow.createCell(3).setCellValue(endTime);


//            newRow.createCell(3).setCellValue(startTime);
//            newRow.createCell(4).setCellValue(endTime);


            try (var fos = Files.newOutputStream(this.curMatchRateFile)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            System.err.println("Error writing matching rate data: " + e.getMessage());
        }
    }
}
