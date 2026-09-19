package com.sentinel.aml.admin;

import com.sentinel.aml.common.AuditService;
import com.sentinel.aml.domain.ExchangeRate;
import com.sentinel.aml.repository.ExchangeRateRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/exchange-rates")
@Tag(name = "Admin - Exchange Rates", description = "Currency-to-base-currency rate table used to normalize amounts")
@PreAuthorize("hasRole('ADMIN')")
public class ExchangeRateAdminController {

    private final ExchangeRateRepository repository;
    private final AuditService auditService;

    public ExchangeRateAdminController(ExchangeRateRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<ExchangeRate> list() {
        return repository.findAll();
    }

    @PostMapping
    public ExchangeRate upsert(@Valid @RequestBody UpsertExchangeRateRequest request) {
        ExchangeRate rate =
                repository.findById(request.currencyCode().toUpperCase()).orElseGet(ExchangeRate::new);
        rate.setCurrencyCode(request.currencyCode().toUpperCase());
        rate.setRateToBase(request.rateToBase());
        rate.setUpdatedAt(Instant.now());
        rate = repository.save(rate);

        auditService.record(
                "EXCHANGE_RATE", rate.getCurrencyCode(), "UPSERTED", Map.of("rateToBase", rate.getRateToBase()));

        return rate;
    }
}
