package com.sentinel.aml.casemgmt;

import com.sentinel.aml.domain.CaseStatus;

public record UpdateCaseRequest(CaseStatus status, String assignedAnalyst, String priority) {
}
