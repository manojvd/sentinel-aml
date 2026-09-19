package com.sentinel.aml.common;

import java.util.List;

public class ValidationFailedException extends RuntimeException {

    private final List<String> details;

    public ValidationFailedException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
