package com.sentinel.aml.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DispositionRequest(@NotNull DispositionAction action, @NotBlank String reason) {
}
