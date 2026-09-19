package com.sentinel.aml.admin;

import com.sentinel.aml.dto.AuditLogDto;
import com.sentinel.aml.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-log")
@Tag(name = "Audit Log", description = "Immutable, insert-only log of alert/case/config state transitions")
@PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_ANALYST')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public Page<AuditLogDto> list(Pageable pageable) {
        return auditLogRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(
                        log ->
                                new AuditLogDto(
                                        log.getId(),
                                        log.getEntityType(),
                                        log.getEntityId(),
                                        log.getAction(),
                                        log.getActorUsername(),
                                        log.getActorRole(),
                                        log.getDetails(),
                                        log.getCreatedAt()));
    }
}
