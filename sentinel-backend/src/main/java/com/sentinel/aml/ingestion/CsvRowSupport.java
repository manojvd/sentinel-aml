package com.sentinel.aml.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.commons.csv.CSVRecord;

final class CsvRowSupport {

    private CsvRowSupport() {
    }

    static String required(CSVRecord row, String column) {
        String value = optional(row, column);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + column);
        }
        return value;
    }

    static String optional(CSVRecord row, String column) {
        if (!row.isMapped(column)) {
            return null;
        }
        String value = row.get(column);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    static String optionalOrDefault(CSVRecord row, String column, String defaultValue) {
        String value = optional(row, column);
        return value == null ? defaultValue : value;
    }

    static LocalDate optionalDate(CSVRecord row, String column) {
        String value = optional(row, column);
        return value == null ? null : LocalDate.parse(value);
    }

    static BigDecimal optionalDecimal(CSVRecord row, String column) {
        String value = optional(row, column);
        return value == null ? null : new BigDecimal(value);
    }

    static BigDecimal optionalDecimalOrDefault(CSVRecord row, String column, BigDecimal defaultValue) {
        BigDecimal value = optionalDecimal(row, column);
        return value == null ? defaultValue : value;
    }

    static boolean optionalBoolean(CSVRecord row, String column) {
        String value = optional(row, column);
        if (value == null) {
            return false;
        }
        return value.equals("1") || value.equalsIgnoreCase("Y") || value.equalsIgnoreCase("true");
    }

    static int optionalInt(CSVRecord row, String column) {
        String value = optional(row, column);
        return value == null ? 0 : Integer.parseInt(value);
    }
}
