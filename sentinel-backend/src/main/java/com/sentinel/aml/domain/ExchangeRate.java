package com.sentinel.aml.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
public class ExchangeRate {

    @Id
    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "rate_to_base", nullable = false)
    private BigDecimal rateToBase;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
