package com.sentinel.aml.alert;

import com.sentinel.aml.domain.AlertStatus;
import com.sentinel.aml.dto.AlertDetailDto;
import com.sentinel.aml.dto.AlertSummaryDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alert queue, evidence/explanation detail, and disposition workflow")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public Page<AlertSummaryDto> list(@RequestParam(required = false) AlertStatus status, Pageable pageable) {
        return alertService.list(status, pageable);
    }

    @GetMapping("/{id}")
    public AlertDetailDto get(@PathVariable UUID id) {
        return alertService.get(id);
    }

    @PostMapping("/{id}/disposition")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_ANALYST')")
    public AlertDetailDto disposition(@PathVariable UUID id, @Valid @RequestBody DispositionRequest request) {
        return alertService.disposition(id, request);
    }
}
