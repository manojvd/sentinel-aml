package com.sentinel.aml.common;

import com.sentinel.aml.domain.AuditLog;
import com.sentinel.aml.repository.AuditLogRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Append-only audit trail. Every alert/case state transition and rule-config change must go
 * through here -- never update or delete an {@link AuditLog} row.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String entityType, String entityId, String action, Map<String, Object> details) {
        CurrentActor actor = CurrentActor.fromSecurityContext();
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setActorUsername(actor.username());
        log.setActorRole(actor.role());
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
