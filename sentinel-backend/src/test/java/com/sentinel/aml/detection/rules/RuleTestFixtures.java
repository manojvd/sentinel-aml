package com.sentinel.aml.detection.rules;

import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.Customer;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.domain.TransactionDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Shared entity builders for detection-rule unit tests -- no DB, plain in-memory objects. */
final class RuleTestFixtures {

    private RuleTestFixtures() {
    }

    static Account account(long id) {
        Account account = new Account();
        account.setId(id);
        account.setAccountId("ACC_%06d".formatted(id));
        account.setCurrency("INR");
        account.setOpenDate(LocalDate.now().minusYears(2));

        Customer customer = new Customer();
        customer.setId(id);
        customer.setCustomerId("CUST_%05d".formatted(id));
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        account.setCustomer(customer);
        return account;
    }

    static Transaction transaction(Account account, String direction, String amount, Instant timestamp) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setAccount(account);
        transaction.setDirection(TransactionDirection.valueOf(direction));
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCurrency("INR");
        transaction.setAmountBaseCurrency(new BigDecimal(amount));
        transaction.setChannel("IMPS");
        transaction.setTxnTimestamp(timestamp);
        return transaction;
    }
}
