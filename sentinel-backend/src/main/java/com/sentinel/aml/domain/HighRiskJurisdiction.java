package com.sentinel.aml.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "high_risk_jurisdictions")
@Getter
@Setter
@NoArgsConstructor
public class HighRiskJurisdiction {

    @Id
    @Column(name = "country_code")
    private String countryCode;

    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();
}
