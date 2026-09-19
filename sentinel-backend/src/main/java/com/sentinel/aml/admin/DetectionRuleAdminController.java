package com.sentinel.aml.admin;

import com.sentinel.aml.common.AuditService;
import com.sentinel.aml.common.NotFoundException;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.domain.DetectionRule;
import com.sentinel.aml.dto.DetectionRuleDto;
import com.sentinel.aml.repository.DetectionRuleRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets compliance/admin tune detection thresholds, weights and enable/disable flags at runtime --
 * no redeploy needed. Backed by {@link RuleConfigService}'s short-TTL cache, which is explicitly
 * invalidated on every write here so a change is visible within one request either way.
 */
@RestController
@RequestMapping("/api/v1/admin/rules")
@Tag(name = "Admin - Detection Rules", description = "View and tune AML rule thresholds/weights without a redeploy")
@PreAuthorize("hasRole('ADMIN')")
public class DetectionRuleAdminController {

    private final DetectionRuleRepository detectionRuleRepository;
    private final RuleConfigService ruleConfigService;
    private final AuditService auditService;

    public DetectionRuleAdminController(
            DetectionRuleRepository detectionRuleRepository,
            RuleConfigService ruleConfigService,
            AuditService auditService) {
        this.detectionRuleRepository = detectionRuleRepository;
        this.ruleConfigService = ruleConfigService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<DetectionRuleDto> list() {
        return detectionRuleRepository.findAll().stream().map(this::toDto).toList();
    }

    @PutMapping("/{ruleCode}")
    public DetectionRuleDto update(@PathVariable String ruleCode, @RequestBody UpdateDetectionRuleRequest request) {
        DetectionRule rule =
                detectionRuleRepository
                        .findById(ruleCode)
                        .orElseThrow(() -> new NotFoundException("No such rule: " + ruleCode));

        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }
        if (request.weight() != null) {
            rule.setWeight(request.weight());
        }
        if (request.config() != null) {
            rule.setConfig(request.config());
        }
        rule.setUpdatedAt(Instant.now());
        rule = detectionRuleRepository.save(rule);
        ruleConfigService.invalidate();

        auditService.record(
                "DETECTION_RULE",
                ruleCode,
                "UPDATED",
                Map.of("enabled", rule.isEnabled(), "weight", rule.getWeight(), "config", rule.getConfig()));

        return toDto(rule);
    }

    private DetectionRuleDto toDto(DetectionRule rule) {
        return new DetectionRuleDto(
                rule.getRuleCode(), rule.getName(), rule.getDescription(), rule.isEnabled(), rule.getWeight(),
                rule.getConfig(), rule.getUpdatedAt());
    }
}
