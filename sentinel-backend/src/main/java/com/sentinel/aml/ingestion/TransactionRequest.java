package com.sentinel.aml.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import com.sentinel.aml.domain.TransactionDirection;

public record TransactionRequest(
        @NotBlank String accountId,
        @NotNull TransactionDirection direction,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        String counterpartyName,
        String counterpartyAccount,
        String counterpartyCountry,
        @NotBlank String channel,
        String jurisdiction,
        Instant txnTimestamp) {
}
