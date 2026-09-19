package com.sentinel.aml.seed;

import com.sentinel.aml.ingestion.AccountIngestionService;
import com.sentinel.aml.ingestion.CustomerIngestionService;
import com.sentinel.aml.ingestion.IngestionSummary;
import com.sentinel.aml.ingestion.TransactionIngestionService;
import com.sentinel.aml.repository.AccountRepository;
import com.sentinel.aml.repository.AlertRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Builds the full demo dataset in one call: the given sample customers/accounts, ~50 more
 * synthetic ones in the same shape, and a transaction batch that deliberately exercises every
 * detection typology plus benign noise up to bulk-load scale. Intended to be called once per
 * environment -- see the README's "call once" note.
 */
@Service
public class DemoSeedService {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedService.class);
    private static final int SYNTHETIC_CUSTOMER_COUNT = 50;
    private static final int SYNTHETIC_CUSTOMER_START_INDEX = 1001;
    private static final int SYNTHETIC_ACCOUNT_START_INDEX = 100001;

    private final CustomerIngestionService customerIngestionService;
    private final AccountIngestionService accountIngestionService;
    private final TransactionIngestionService transactionIngestionService;
    private final AccountRepository accountRepository;
    private final AlertRepository alertRepository;
    private final SyntheticDataGenerator generator;

    public DemoSeedService(
            CustomerIngestionService customerIngestionService,
            AccountIngestionService accountIngestionService,
            TransactionIngestionService transactionIngestionService,
            AccountRepository accountRepository,
            AlertRepository alertRepository,
            SyntheticDataGenerator generator) {
        this.customerIngestionService = customerIngestionService;
        this.accountIngestionService = accountIngestionService;
        this.transactionIngestionService = transactionIngestionService;
        this.accountRepository = accountRepository;
        this.alertRepository = alertRepository;
        this.generator = generator;
    }

    public DemoSeedSummary seed(int totalTransactions) throws IOException {
        long start = System.currentTimeMillis();

        IngestionSummary givenCustomers = ingestClasspathCsv("seed/customers.csv", customerIngestionService::ingest);
        IngestionSummary givenAccounts = ingestClasspathCsv("seed/accounts.csv", accountIngestionService::ingest);

        List<String> syntheticCustomerIds =
                generator.generateCustomerIds(SYNTHETIC_CUSTOMER_COUNT, SYNTHETIC_CUSTOMER_START_INDEX);
        String syntheticCustomersCsv =
                generator.generateCustomersCsv(SYNTHETIC_CUSTOMER_COUNT, SYNTHETIC_CUSTOMER_START_INDEX);
        IngestionSummary syntheticCustomers = customerIngestionService.ingest(new StringReader(syntheticCustomersCsv));

        String syntheticAccountsCsv = generator.generateAccountsCsv(syntheticCustomerIds, SYNTHETIC_ACCOUNT_START_INDEX);
        IngestionSummary syntheticAccounts = accountIngestionService.ingest(new StringReader(syntheticAccountsCsv));

        List<String> allAccountIds = new ArrayList<>(accountRepository.findAll().stream().map(a -> a.getAccountId()).toList());

        String transactionsCsv = generator.generateTransactionsCsv(allAccountIds, totalTransactions);
        IngestionSummary transactions = transactionIngestionService.ingestBulk(new StringReader(transactionsCsv));

        IngestionSummary combinedCustomers = combine(givenCustomers, syntheticCustomers);
        IngestionSummary combinedAccounts = combine(givenAccounts, syntheticAccounts);

        long totalAlerts = alertRepository.count();
        long elapsed = System.currentTimeMillis() - start;
        log.info(
                "Demo seed complete: {} customers, {} accounts, {} transactions, {} total alerts, in {}ms",
                combinedCustomers.accepted(),
                combinedAccounts.accepted(),
                transactions.accepted(),
                totalAlerts,
                elapsed);

        return new DemoSeedSummary(combinedCustomers, combinedAccounts, transactions, totalAlerts, elapsed);
    }

    private IngestionSummary ingestClasspathCsv(String path, CsvIngestor ingestor) throws IOException {
        try (var reader = new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
            return ingestor.ingest(reader);
        }
    }

    private IngestionSummary combine(IngestionSummary a, IngestionSummary b) {
        List<String> errors = new ArrayList<>(a.errors());
        errors.addAll(b.errors());
        return new IngestionSummary(
                a.accepted() + b.accepted(), a.rejected() + b.rejected(), errors, a.elapsedMillis() + b.elapsedMillis());
    }

    @FunctionalInterface
    private interface CsvIngestor {
        IngestionSummary ingest(java.io.Reader reader) throws IOException;
    }
}
