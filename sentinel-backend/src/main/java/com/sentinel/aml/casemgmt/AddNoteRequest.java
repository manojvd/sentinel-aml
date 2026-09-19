package com.sentinel.aml.casemgmt;

import jakarta.validation.constraints.NotBlank;

public record AddNoteRequest(@NotBlank String note) {
}
