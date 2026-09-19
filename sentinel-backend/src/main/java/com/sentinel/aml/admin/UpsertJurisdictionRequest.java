package com.sentinel.aml.admin;

import jakarta.validation.constraints.NotBlank;

public record UpsertJurisdictionRequest(@NotBlank String countryCode, String reason, boolean active) {
}
