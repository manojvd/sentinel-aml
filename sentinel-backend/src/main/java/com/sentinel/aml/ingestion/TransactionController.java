package com.sentinel.aml.ingestion;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Single-transaction (streaming) ingestion, evaluated synchronously")
public class TransactionController {

    private final TransactionIngestionService transactionIngestionService;

    public TransactionController(TransactionIngestionService transactionIngestionService) {
        this.transactionIngestionService = transactionIngestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_ANALYST')")
    public TransactionResult ingest(@Valid @RequestBody TransactionRequest request) {
        return transactionIngestionService.ingestSingle(request);
    }
}
