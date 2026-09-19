package com.sentinel.aml.ingestion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ingest")
@Tag(name = "Ingestion", description = "Bulk CSV load for customers, accounts and transactions")
@PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_ANALYST')")
public class IngestionController {

    private final CustomerIngestionService customerIngestionService;
    private final AccountIngestionService accountIngestionService;
    private final TransactionIngestionService transactionIngestionService;

    public IngestionController(
            CustomerIngestionService customerIngestionService,
            AccountIngestionService accountIngestionService,
            TransactionIngestionService transactionIngestionService) {
        this.customerIngestionService = customerIngestionService;
        this.accountIngestionService = accountIngestionService;
        this.transactionIngestionService = transactionIngestionService;
    }

    @PostMapping(value = "/customers", consumes = "multipart/form-data")
    public IngestionSummary ingestCustomers(@RequestParam("file") MultipartFile file) throws IOException {
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return customerIngestionService.ingest(reader);
        }
    }

    @PostMapping(value = "/accounts", consumes = "multipart/form-data")
    public IngestionSummary ingestAccounts(@RequestParam("file") MultipartFile file) throws IOException {
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return accountIngestionService.ingest(reader);
        }
    }

    @PostMapping(value = "/transactions", consumes = "multipart/form-data")
    public IngestionSummary ingestTransactions(@RequestParam("file") MultipartFile file) throws IOException {
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return transactionIngestionService.ingestBulk(reader);
        }
    }
}
