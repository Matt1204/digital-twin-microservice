package com.example.centralOperator.service;

import com.example.centralOperator.listener.OrderInitListener;
import com.example.centralOperator.model.TaxiOrder;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AllOrders {

    private final List<TaxiOrder> allOrders = new CopyOnWriteArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(AllOrders.class);

    @PostConstruct
    public void init() {
        logger.info("AllOrders initializing......");
        loadOrdersFromExcel("2010-06-01+3_trips.xlsx");
        logger.info("AllOrders initializing Done.");

    }

    public List<TaxiOrder> getAllOrders() {
        return List.copyOf(allOrders);
    }

    public void loadOrdersFromExcel(String fileName) {
//        String fileName = "2010-06-01+3_trips.xlsx";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row (row index 0)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Date pickupTime = row.getCell(0).getDateCellValue();
                double pickupLat = row.getCell(1).getNumericCellValue();
                double pickupLon = row.getCell(2).getNumericCellValue();
                double dropoffLat = row.getCell(3).getNumericCellValue();
                double dropoffLon = row.getCell(4).getNumericCellValue();
                double tripDistance = row.getCell(5).getNumericCellValue();
                double tripIncome = row.getCell(6).getNumericCellValue();

                String orderId = "" + i; // Generate a simple orderId
                TaxiOrder order = new TaxiOrder(orderId, pickupLon, pickupLat, dropoffLon, dropoffLat, pickupTime, tripDistance, tripIncome);
                allOrders.add(order);
            }

            int count = this.getOrderCount();
            logger.info("AllOrders read {} from {}", count, fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getOrderCount() {
        return allOrders.size();
    }

    public boolean isEmpty() {
        return allOrders.isEmpty();
    }

    public void clearOrders() {
        allOrders.clear();
    }

    public TaxiOrder getOrder(int index) {
        if (index >= 0 && index < allOrders.size()) {
            return allOrders.get(index);
        }
        throw new IndexOutOfBoundsException("Invalid order index: " + index);
    }
}