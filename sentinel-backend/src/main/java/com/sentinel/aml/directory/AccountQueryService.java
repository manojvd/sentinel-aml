package com.sentinel.aml.directory;

import com.sentinel.aml.common.NotFoundException;
import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.Customer;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.dto.AccountSummaryDto;
import com.sentinel.aml.dto.TransactionDto;
import com.sentinel.aml.repository.AccountRepository;
import com.sentinel.aml.repository.CustomerRepository;
import com.sentinel.aml.repository.TransactionRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AccountQueryService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public AccountQueryService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<AccountSummaryDto> listForCustomer(String customerBusinessId) {
        Customer customer =
                customerRepository
                        .findByCustomerId(customerBusinessId)
                        .orElseThrow(() -> new NotFoundException("No such customer: " + customerBusinessId));
        return accountRepository.findByCustomerId(customer.getId()).stream().map(this::toSummary).toList();
    }

    public AccountSummaryDto get(String accountId) {
        return toSummary(findEntity(accountId));
    }

    public Page<TransactionDto> timeline(String accountId, Pageable pageable) {
        Account account = findEntity(accountId);
        return transactionRepository
                .findByAccount_IdOrderByTxnTimestampDesc(account.getId(), pageable)
                .map(txn -> toDto(txn, accountId));
    }

    private Account findEntity(String accountId) {
        return accountRepository
                .findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("No such account: " + accountId));
    }

    private AccountSummaryDto toSummary(Account account) {
        return new AccountSummaryDto(
                account.getId(),
                account.getAccountId(),
                account.getAccountType(),
                account.getAccountStatus(),
                account.getCurrency(),
                account.getCurrentBalance(),
                account.getAccountTier());
    }

    private TransactionDto toDto(Transaction transaction, String accountId) {
        return new TransactionDto(
                transaction.getId(),
                accountId,
                transaction.getDirection(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getAmountBaseCurrency(),
                transaction.getCounterpartyName(),
                transaction.getCounterpartyCountry(),
                transaction.getChannel(),
                transaction.getJurisdiction(),
                transaction.getTxnTimestamp());
    }
}
