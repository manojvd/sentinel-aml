package com.sentinel.aml.detection;

import com.sentinel.aml.common.ValidationFailedException;
import com.sentinel.aml.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/** Normalizes any supported currency amount to the configured base currency (INR). */
@Service
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public BigDecimal toBaseCurrency(BigDecimal amount, String currencyCode) {
        var rate =
                exchangeRateRepository
                        .findById(currencyCode)
                        .orElseThrow(
                                () ->
                                        new ValidationFailedException(
                                                "Unsupported currency: " + currencyCode,
                                                List.of("currency: no exchange rate configured for " + currencyCode)));
        return amount.multiply(rate.getRateToBase()).setScale(2, RoundingMode.HALF_UP);
    }
}
