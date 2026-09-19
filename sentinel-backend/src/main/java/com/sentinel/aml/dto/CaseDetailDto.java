package com.sentinel.aml.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaseDetailDto(
        UUID id,
        String customerId,
        String status,
        String priority,
        String assignedAnalyst,
        String summary,
        List<UUID> alertIds,
        Instant createdAt,
        Instant updatedAt) {
}
