package com.sentinel.aml.dto;

import com.sentinel.aml.domain.TransactionDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDto(
        UUID id,
        String accountId,
        TransactionDirection direction,
        BigDecimal amount,
        String currency,
        BigDecimal amountBaseCurrency,
        String counterpartyName,
        String counterpartyCountry,
        String channel,
        String jurisdiction,
        Instant txnTimestamp) {
}
