package com.sentinel.aml.casemgmt;

import com.sentinel.aml.dto.CaseDetailDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Cases", description = "Case management workflow: create from alerts, assign, disposition, notes")
@PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_ANALYST')")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public Page<CaseDetailDto> list(Pageable pageable) {
        return caseService.list(pageable);
    }

    @GetMapping("/{id}")
    public CaseDetailDto get(@PathVariable UUID id) {
        return caseService.get(id);
    }

    @PostMapping
    public CaseDetailDto create(@Valid @RequestBody CreateCaseRequest request) {
        return caseService.create(request);
    }

    @PutMapping("/{id}")
    public CaseDetailDto update(@PathVariable UUID id, @RequestBody UpdateCaseRequest request) {
        return caseService.update(id, request);
    }

    @PostMapping("/{id}/notes")
    public CaseDetailDto addNote(@PathVariable UUID id, @Valid @RequestBody AddNoteRequest request) {
        return caseService.addNote(id, request);
    }
}
