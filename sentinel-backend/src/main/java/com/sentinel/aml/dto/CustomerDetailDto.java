package com.sentinel.aml.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerDetailDto(
        Long id,
        String customerId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        String city,
        String state,
        String country,
        String occupation,
        BigDecimal annualIncome,
        String kycStatus,
        String riskRating,
        boolean politicallyExposed,
        String customerSegment,
        boolean piiMasked) {
}
