package com.sentinel.aml.dto;

import java.time.Instant;
import java.util.Map;

public record AuditLogDto(
        Long id,
        String entityType,
        String entityId,
        String action,
        String actorUsername,
        String actorRole,
        Map<String, Object> details,
        Instant createdAt) {
}
