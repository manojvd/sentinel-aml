package com.sentinel.aml.admin;

import com.sentinel.aml.common.AuditService;
import com.sentinel.aml.detection.HighRiskJurisdictionService;
import com.sentinel.aml.domain.HighRiskJurisdiction;
import com.sentinel.aml.repository.HighRiskJurisdictionRepository;
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
@RequestMapping("/api/v1/admin/high-risk-jurisdictions")
@Tag(name = "Admin - High-Risk Jurisdictions", description = "The configurable sanctions/high-risk country list")
@PreAuthorize("hasRole('ADMIN')")
public class HighRiskJurisdictionAdminController {

    private final HighRiskJurisdictionRepository repository;
    private final HighRiskJurisdictionService highRiskJurisdictionService;
    private final AuditService auditService;

    public HighRiskJurisdictionAdminController(
            HighRiskJurisdictionRepository repository,
            HighRiskJurisdictionService highRiskJurisdictionService,
            AuditService auditService) {
        this.repository = repository;
        this.highRiskJurisdictionService = highRiskJurisdictionService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<HighRiskJurisdiction> list() {
        return repository.findAll();
    }

    @PostMapping
    public HighRiskJurisdiction upsert(@Valid @RequestBody UpsertJurisdictionRequest request) {
        HighRiskJurisdiction jurisdiction =
                repository.findById(request.countryCode().toUpperCase()).orElseGet(HighRiskJurisdiction::new);
        jurisdiction.setCountryCode(request.countryCode().toUpperCase());
        jurisdiction.setReason(request.reason());
        jurisdiction.setActive(request.active());
        if (jurisdiction.getAddedAt() == null) {
            jurisdiction.setAddedAt(Instant.now());
        }
        jurisdiction = repository.save(jurisdiction);
        highRiskJurisdictionService.invalidate();

        auditService.record(
                "HIGH_RISK_JURISDICTION",
                jurisdiction.getCountryCode(),
                "UPSERTED",
                Map.of("active", jurisdiction.isActive(), "reason", String.valueOf(jurisdiction.getReason())));

        return jurisdiction;
    }
}
