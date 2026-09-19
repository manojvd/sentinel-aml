package com.sentinel.aml.casemgmt;

import com.sentinel.aml.common.AuditService;
import com.sentinel.aml.common.CurrentActor;
import com.sentinel.aml.common.NotFoundException;
import com.sentinel.aml.common.ValidationFailedException;
import com.sentinel.aml.domain.Alert;
import com.sentinel.aml.domain.Case;
import com.sentinel.aml.domain.Customer;
import com.sentinel.aml.dto.CaseDetailDto;
import com.sentinel.aml.repository.AlertRepository;
import com.sentinel.aml.repository.CaseRepository;
import com.sentinel.aml.repository.CustomerRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CaseService {

    private final CaseRepository caseRepository;
    private final AlertRepository alertRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CaseService(
            CaseRepository caseRepository,
            AlertRepository alertRepository,
            CustomerRepository customerRepository,
            AuditService auditService) {
        this.caseRepository = caseRepository;
        this.alertRepository = alertRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    public Page<CaseDetailDto> list(Pageable pageable) {
        return caseRepository.findAllByOrderByUpdatedAtDesc(pageable).map(this::toDto);
    }

    public CaseDetailDto get(UUID id) {
        return toDto(findEntity(id));
    }

    @Transactional
    public CaseDetailDto create(CreateCaseRequest request) {
        Customer customer =
                customerRepository
                        .findByCustomerId(request.customerId())
                        .orElseThrow(() -> new NotFoundException("No such customer: " + request.customerId()));

        List<Alert> alerts =
                request.alertIds().stream()
                        .map(
                                alertId ->
                                        alertRepository
                                                .findById(alertId)
                                                .orElseThrow(() -> new NotFoundException("No such alert: " + alertId)))
                        .toList();

        boolean allSameCustomer =
                alerts.stream().allMatch(a -> a.getCustomer().getId().equals(customer.getId()));
        if (!allSameCustomer) {
            throw new ValidationFailedException(
                    "All alerts in a case must belong to the same customer", List.of("alertIds"));
        }

        Case aCase = new Case();
        aCase.setCustomer(customer);
        aCase.setPriority(request.priority() == null ? "MEDIUM" : request.priority());
        aCase.setSummary(request.summary());
        aCase.getAlerts().addAll(alerts);
        aCase = caseRepository.save(aCase);

        auditService.record(
                "CASE",
                aCase.getId().toString(),
                "CREATED",
                Map.of("customerId", customer.getCustomerId(), "alertIds", request.alertIds()));

        return toDto(aCase);
    }

    @Transactional
    public CaseDetailDto update(UUID id, UpdateCaseRequest request) {
        Case aCase = findEntity(id);
        if (request.status() != null) {
            aCase.setStatus(request.status());
        }
        if (request.assignedAnalyst() != null) {
            aCase.setAssignedAnalyst(request.assignedAnalyst());
        }
        if (request.priority() != null) {
            aCase.setPriority(request.priority());
        }
        aCase.setUpdatedAt(Instant.now());
        aCase = caseRepository.save(aCase);

        auditService.record(
                "CASE",
                aCase.getId().toString(),
                "UPDATED",
                Map.of(
                        "status", aCase.getStatus().name(),
                        "assignedAnalyst", String.valueOf(aCase.getAssignedAnalyst())));

        return toDto(aCase);
    }

    @Transactional
    public CaseDetailDto addNote(UUID id, AddNoteRequest request) {
        Case aCase = findEntity(id);
        CurrentActor actor = CurrentActor.fromSecurityContext();
        String stamped =
                "[%s %s] %s".formatted(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), actor.username(), request.note());
        String existing = aCase.getSummary();
        aCase.setSummary(existing == null || existing.isBlank() ? stamped : existing + "\n" + stamped);
        aCase.setUpdatedAt(Instant.now());
        aCase = caseRepository.save(aCase);

        auditService.record("CASE", aCase.getId().toString(), "NOTE_ADDED", Map.of("note", request.note()));

        return toDto(aCase);
    }

    private Case findEntity(UUID id) {
        return caseRepository.findById(id).orElseThrow(() -> new NotFoundException("No such case: " + id));
    }

    private CaseDetailDto toDto(Case aCase) {
        return new CaseDetailDto(
                aCase.getId(),
                aCase.getCustomer().getCustomerId(),
                aCase.getStatus().name(),
                aCase.getPriority(),
                aCase.getAssignedAnalyst(),
                aCase.getSummary(),
                aCase.getAlerts().stream().map(Alert::getId).toList(),
                aCase.getCreatedAt(),
                aCase.getUpdatedAt());
    }
}
