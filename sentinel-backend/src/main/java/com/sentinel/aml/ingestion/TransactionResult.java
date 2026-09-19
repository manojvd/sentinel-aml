package com.sentinel.aml.ingestion;

import com.sentinel.aml.domain.Alert;
import com.sentinel.aml.domain.Transaction;
import java.util.Optional;

public record TransactionResult(Transaction transaction, Optional<Alert> alert) {
}
