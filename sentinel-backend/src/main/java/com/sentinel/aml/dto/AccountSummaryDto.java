package com.sentinel.aml.dto;

import java.math.BigDecimal;

public record AccountSummaryDto(
        Long id,
        String accountId,
        String accountType,
        String accountStatus,
        String currency,
        BigDecimal currentBalance,
        String accountTier) {
}
