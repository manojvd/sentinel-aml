package com.sentinel.aml.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpsertExchangeRateRequest(@NotBlank String currencyCode, @Positive BigDecimal rateToBase) {
}
