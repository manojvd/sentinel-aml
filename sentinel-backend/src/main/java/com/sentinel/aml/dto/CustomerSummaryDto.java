package com.sentinel.aml.dto;

public record CustomerSummaryDto(
        Long id,
        String customerId,
        String displayName,
        String riskRating,
        String kycStatus,
        String country,
        String customerSegment,
        boolean politicallyExposed) {
}
