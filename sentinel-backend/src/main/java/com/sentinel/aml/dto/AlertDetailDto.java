package com.sentinel.aml.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertDetailDto(
        UUID id,
        String customerId,
        String customerDisplayName,
        String accountId,
        int riskScore,
        String status,
        String dispositionReason,
        String dispositionActor,
        Instant dispositionedAt,
        List<AlertEvidenceDto> evidence,
        Instant createdAt,
        Instant updatedAt) {
}
