
package com.pgconnect.service;

import com.pgconnect.model.Pg;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class PgExcelService {

    public record ImportResult(List<Pg> pgs, List<String> errors) {
    }

    // ---------------- EXCEL IMPORT ----------------

    public ImportResult importExcel(MultipartFile file) throws IOException {

        List<Pg> pgs = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheet("Import_Template");

            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException("Excel header row is missing.");
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = new HashMap<>();

            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell)
                        .trim().toLowerCase();
                columns.put(header, cell.getColumnIndex());
            }

            String[] requiredHeaders = {
                    "name", "gender", "rent", "sharing",
                    "amenities", "area", "address", "phone",
                    "lat", "lng"
            };

            for (String header : requiredHeaders) {
                if (!columns.containsKey(header)) {
                    throw new IllegalArgumentException(
                            "Missing Excel column: " + header);
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                try {
                    Pg pg = new Pg();

                    String name = getValue(row, columns, "name", formatter);
                    String gender = getValue(row, columns, "gender", formatter)
                            .toLowerCase();

                    String area = getValue(row, columns, "area", formatter);
                    String address = getValue(row, columns, "address", formatter);

                    if (area.isBlank()) {
                        throw new IllegalArgumentException("Area is required");
                    }

                    if (address.isBlank()) {
                        throw new IllegalArgumentException("Address is required");
                    }

                    if (gender.equals("co-living")) {
                        gender = "unisex";
                    }

                    if (!List.of("male", "female", "unisex").contains(gender)) {
                        throw new IllegalArgumentException(
                                "Gender must be male, female or unisex");
                    }

                    int rent = Integer.parseInt(
                            getValue(row, columns, "rent", formatter)
                                    .replace(",", "")
                                    .replace("₹", "")
                                    .trim());

                    if (rent <= 0) {
                        throw new IllegalArgumentException(
                                "Rent must be greater than zero");
                    }

                    double lat = Double.parseDouble(
                            getValue(row, columns, "lat", formatter));

                    double lng = Double.parseDouble(
                            getValue(row, columns, "lng", formatter));

                    if (!Double.isFinite(lat) || !Double.isFinite(lng)
                            || lat < -90 || lat > 90
                            || lng < -180 || lng > 180) {

                        throw new IllegalArgumentException(
                                "Invalid latitude or longitude");
                    }

                    pg.setName(name);
                    pg.setGender(gender);
                    pg.setRent(rent);
                    pg.setSharing(parseSharing(
                            getValue(row, columns, "sharing", formatter)));
                    pg.setAmenities(parseAmenities(
                            getValue(row, columns, "amenities", formatter)));
                    pg.setArea(area);
                    pg.setAddress(address);
                    pg.setPhone(getValue(row, columns, "phone", formatter));

                    // GeoJSON coordinate order: longitude first, latitude second
                    pg.setLocation(new GeoJsonPoint(lng, lat));

                    pgs.add(pg);

                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return new ImportResult(pgs, errors);
    }

    private String getValue(
            Row row,
            Map<String, Integer> columns,
            String header,
            DataFormatter formatter) {

        Integer index = columns.get(header);

        if (index == null) {
            return "";
        }

        Cell cell = row.getCell(index);

        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> parseSharing(String value) {

        List<Integer> sharing = new ArrayList<>();

        if (value == null || value.isBlank()) {
            return sharing;
        }

        for (String item : value.split(",")) {
            String part = item.trim().toLowerCase();

            switch (part) {
                case "single" -> sharing.add(1);
                case "double" -> sharing.add(2);
                case "triple" -> sharing.add(3);
                case "four sharing", "four-sharing", "4 sharing" ->
                    sharing.add(4);
                default -> {
                    int number = Integer.parseInt(part);
                    if (number < 1 || number > 10) {
                        throw new IllegalArgumentException(
                                "Sharing value must be between 1 and 10");
                    }
                    sharing.add(number);
                }
            }
        }

        return sharing;
    }

    private List<String> parseAmenities(String value) {

        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }

        List<String> amenities = new ArrayList<>();

        for (String item : value.split(",")) {
            if (!item.trim().isBlank()) {
                amenities.add(item.trim());
            }
        }

        return amenities;
    }

    // ---------------- EXCEL EXPORT ----------------

    public byte[] exportExcel(List<Pg> pgs) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("PG_Listings");

            String[] headers = {
                    "name", "gender", "rent", "sharing",
                    "amenities", "area", "address", "phone",
                    "lat", "lng"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;

            for (Pg pg : pgs) {

                Row row = sheet.createRow(rowIndex++);

                setCell(row, 0, pg.getName());
                setCell(row, 1, pg.getGender());
                setCell(row, 2, pg.getRent());
                setCell(row, 3, formatSharing(pg.getSharing()));
                setCell(row, 4, String.join(", ",
                        pg.getAmenities() == null
                                ? Collections.emptyList()
                                : pg.getAmenities()));
                setCell(row, 5, pg.getArea());
                setCell(row, 6, pg.getAddress());
                setCell(row, 7, pg.getPhone());

                if (pg.getLocation() != null) {
                    setCell(row, 8, pg.getLocation().getY()); // latitude
                    setCell(row, 9, pg.getLocation().getX()); // longitude
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String formatSharing(List<Integer> sharing) {

        if (sharing == null || sharing.isEmpty()) {
            return "";
        }

        List<String> values = new ArrayList<>();

        for (Integer number : sharing) {
            values.add(switch (number) {
                case 1 -> "Single";
                case 2 -> "Double";
                case 3 -> "Triple";
                case 4 -> "Four Sharing";
                default -> number + " Sharing";
            });
        }

        return String.join(", ", values);
    }

    private void setCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void setCell(Row row, int column, int value) {
        row.createCell(column).setCellValue(value);
    }

    private void setCell(Row row, int column, double value) {
        row.createCell(column).setCellValue(value);
    }
}
