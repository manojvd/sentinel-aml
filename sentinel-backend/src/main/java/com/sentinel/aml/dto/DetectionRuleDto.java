package com.sentinel.aml.dto;

import java.time.Instant;
import java.util.Map;

public record DetectionRuleDto(
        String ruleCode,
        String name,
        String description,
        boolean enabled,
        int weight,
        Map<String, Object> config,
        Instant updatedAt) {
}
