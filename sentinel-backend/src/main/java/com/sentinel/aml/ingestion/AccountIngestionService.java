package com.sentinel.aml.ingestion;

import static com.sentinel.aml.ingestion.CsvRowSupport.optional;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalBoolean;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalDate;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalDecimal;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalDecimalOrDefault;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalInt;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalOrDefault;
import static com.sentinel.aml.ingestion.CsvRowSupport.required;

import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.Customer;
import com.sentinel.aml.domain.IngestionError;
import com.sentinel.aml.repository.AccountRepository;
import com.sentinel.aml.repository.CustomerRepository;
import com.sentinel.aml.repository.IngestionErrorRepository;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

@Service
public class AccountIngestionService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final IngestionErrorRepository ingestionErrorRepository;

    public AccountIngestionService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            IngestionErrorRepository ingestionErrorRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.ingestionErrorRepository = ingestionErrorRepository;
    }

    public IngestionSummary ingest(Reader csvReader) throws IOException {
        long start = System.currentTimeMillis();
        int accepted = 0;
        List<String> errors = new ArrayList<>();

        try (CSVParser parser =
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(csvReader)) {
            for (CSVRecord record : parser) {
                try {
                    Account account = mapRow(record);
                    if (accountRepository.existsByAccountId(account.getAccountId())) {
                        throw new IllegalArgumentException("Duplicate account_id: " + account.getAccountId());
                    }
                    accountRepository.save(account);
                    accepted++;
                } catch (Exception ex) {
                    errors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                    ingestionErrorRepository.save(new IngestionError("ACCOUNT", record.toString(), ex.getMessage()));
                }
            }
        }

        return new IngestionSummary(accepted, errors.size(), errors, System.currentTimeMillis() - start);
    }

    private Account mapRow(CSVRecord row) {
        String customerBusinessId = required(row, "customer_id");
        Customer customer =
                customerRepository
                        .findByCustomerId(customerBusinessId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown customer_id: " + customerBusinessId));

        Account account = new Account();
        account.setAccountId(required(row, "account_id"));
        account.setCustomer(customer);
        account.setAccountType(required(row, "account_type"));
        account.setAccountStatus(optionalOrDefault(row, "account_status", "ACTIVE"));
        account.setCurrency(required(row, "currency"));
        account.setOpenDate(optionalDate(row, "open_date"));
        if (account.getOpenDate() == null) {
            throw new IllegalArgumentException("Missing required field: open_date");
        }
        account.setCloseDate(optionalDate(row, "close_date"));
        account.setBranchCode(optional(row, "branch_code"));
        account.setBranchCity(optional(row, "branch_city"));
        account.setCurrentBalance(optionalDecimalOrDefault(row, "current_balance", BigDecimal.ZERO));
        account.setAvgMonthlyBalance6m(optionalDecimal(row, "avg_monthly_balance_6m"));
        account.setCreditLimit(optionalDecimal(row, "credit_limit"));
        account.setCreditUtilizationPct(optionalDecimal(row, "credit_utilization_pct"));
        account.setOverdraftEnabled(optionalBoolean(row, "overdraft_enabled"));
        account.setCardType(optional(row, "card_type"));
        account.setJointAccount(optionalBoolean(row, "is_joint_account"));
        account.setMobileBankingEnrolled(optionalBoolean(row, "mobile_banking_enrolled"));
        int avgTxnCount = optionalInt(row, "avg_monthly_txn_count");
        account.setAvgMonthlyTxnCount(avgTxnCount == 0 ? null : avgTxnCount);
        account.setAccountTier(optional(row, "account_tier"));
        return account;
    }
}
