package com.sentinel.aml.ingestion;

import static com.sentinel.aml.ingestion.CsvRowSupport.optional;
import static com.sentinel.aml.ingestion.CsvRowSupport.required;

import com.sentinel.aml.detection.DetectionEngine;
import com.sentinel.aml.detection.ExchangeRateService;
import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.IngestionError;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.domain.TransactionDirection;
import com.sentinel.aml.repository.AccountRepository;
import com.sentinel.aml.repository.IngestionErrorRepository;
import com.sentinel.aml.repository.TransactionRepository;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionIngestionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IngestionErrorRepository ingestionErrorRepository;
    private final ExchangeRateService exchangeRateService;
    private final DetectionEngine detectionEngine;
    private final ExecutorService detectionExecutor;

    public TransactionIngestionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            IngestionErrorRepository ingestionErrorRepository,
            ExchangeRateService exchangeRateService,
            DetectionEngine detectionEngine,
            ExecutorService detectionExecutor) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ingestionErrorRepository = ingestionErrorRepository;
        this.exchangeRateService = exchangeRateService;
        this.detectionEngine = detectionEngine;
        this.detectionExecutor = detectionExecutor;
    }

    @Transactional
    public TransactionResult ingestSingle(TransactionRequest request) {
        Account account =
                accountRepository
                        .findByAccountId(request.accountId())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown account_id: " + request.accountId()));

        Transaction transaction = buildTransaction(account, request);
        transaction = transactionRepository.save(transaction);

        Optional<com.sentinel.aml.domain.Alert> alert = detectionEngine.evaluate(transaction.getId());
        return new TransactionResult(transaction, alert);
    }

    public IngestionSummary ingestBulk(Reader csvReader) throws IOException {
        long start = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<Transaction> saved = new ArrayList<>();

        try (CSVParser parser =
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(csvReader)) {
            for (CSVRecord record : parser) {
                try {
                    saved.add(saveRow(record));
                } catch (Exception ex) {
                    errors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                    ingestionErrorRepository.save(new IngestionError("TRANSACTION", record.toString(), ex.getMessage()));
                }
            }
        }

        // Phase 2: all rows are committed, so window-based rules see the full batch regardless of
        // per-account evaluation order -- safe to fan out across the detection thread pool.
        List<CompletableFuture<Void>> futures =
                saved.stream()
                        .map(
                                txn ->
                                        CompletableFuture.runAsync(
                                                () -> {
                                                    try {
                                                        detectionEngine.evaluate(txn.getId());
                                                    } catch (Exception ex) {
                                                        log.error("Detection failed for transaction {}", txn.getId(), ex);
                                                    }
                                                },
                                                detectionExecutor))
                        .toList();
        futures.forEach(CompletableFuture::join);

        return new IngestionSummary(saved.size(), errors.size(), errors, System.currentTimeMillis() - start);
    }

    @Transactional
    protected Transaction saveRow(CSVRecord record) {
        String accountBusinessId = required(record, "account_id");
        Account account =
                accountRepository
                        .findByAccountId(accountBusinessId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown account_id: " + accountBusinessId));

        TransactionDirection direction = TransactionDirection.valueOf(required(record, "direction").toUpperCase());
        BigDecimal amount = new BigDecimal(required(record, "amount"));
        String currency = required(record, "currency");
        String channel = required(record, "channel");
        String timestampValue = optional(record, "txn_timestamp");
        Instant txnTimestamp = timestampValue == null ? Instant.now() : Instant.parse(timestampValue);

        TransactionRequest request =
                new TransactionRequest(
                        accountBusinessId,
                        direction,
                        amount,
                        currency,
                        optional(record, "counterparty_name"),
                        optional(record, "counterparty_account"),
                        optional(record, "counterparty_country"),
                        channel,
                        optional(record, "jurisdiction"),
                        txnTimestamp);

        Transaction transaction = buildTransaction(account, request);
        return transactionRepository.save(transaction);
    }

    private Transaction buildTransaction(Account account, TransactionRequest request) {
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setDirection(request.direction());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        transaction.setAmountBaseCurrency(exchangeRateService.toBaseCurrency(request.amount(), request.currency()));
        transaction.setCounterpartyName(request.counterpartyName());
        transaction.setCounterpartyAccount(request.counterpartyAccount());
        transaction.setCounterpartyCountry(request.counterpartyCountry());
        transaction.setChannel(request.channel());
        transaction.setJurisdiction(request.jurisdiction());
        transaction.setTxnTimestamp(request.txnTimestamp() == null ? Instant.now() : request.txnTimestamp());
        return transaction;
    }
}
