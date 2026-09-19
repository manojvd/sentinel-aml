package com.sentinel.aml.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sentinel.aml.common.ValidationFailedException;
import com.sentinel.aml.domain.ExchangeRate;
import com.sentinel.aml.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private ExchangeRateRepository exchangeRateRepository;

    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        service = new ExchangeRateService(exchangeRateRepository);
    }

    @Test
    void normalizesToBaseCurrencyUsingTheConfiguredRate() {
        ExchangeRate usd = new ExchangeRate();
        usd.setCurrencyCode("USD");
        usd.setRateToBase(new BigDecimal("91.50"));
        when(exchangeRateRepository.findById("USD")).thenReturn(Optional.of(usd));

        BigDecimal result = service.toBaseCurrency(new BigDecimal("100"), "USD");

        assertThat(result).isEqualByComparingTo(new BigDecimal("9150.00"));
    }

    @Test
    void inrPassesThroughUnchangedAtRateOne() {
        ExchangeRate inr = new ExchangeRate();
        inr.setCurrencyCode("INR");
        inr.setRateToBase(BigDecimal.ONE);
        when(exchangeRateRepository.findById("INR")).thenReturn(Optional.of(inr));

        BigDecimal result = service.toBaseCurrency(new BigDecimal("500.00"), "INR");

        assertThat(result).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void rejectsAnUnsupportedCurrency() {
        when(exchangeRateRepository.findById("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toBaseCurrency(new BigDecimal("10"), "XYZ"))
                .isInstanceOf(ValidationFailedException.class);
    }
}
