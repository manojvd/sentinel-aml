package com.sentinel.aml.casemgmt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CreateCaseRequest(
        @NotBlank String customerId, @NotEmpty List<UUID> alertIds, String priority, String summary) {
}
