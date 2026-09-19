package com.sentinel.aml.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StructuringRuleTest {

    @Mock private RuleConfigService ruleConfigService;
    @Mock private TransactionRepository transactionRepository;

    private StructuringRule rule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new StructuringRule(ruleConfigService, transactionRepository);
        account = RuleTestFixtures.account(1);
        lenient()
                .when(ruleConfigService.bigDecimal(eq(StructuringRule.RULE_CODE), eq("minAmountBase"), any()))
                .thenReturn(new BigDecimal("9000"));
        lenient()
                .when(ruleConfigService.bigDecimal(eq(StructuringRule.RULE_CODE), eq("maxAmountBase"), any()))
                .thenReturn(new BigDecimal("9999"));
        lenient()
                .when(ruleConfigService.integer(eq(StructuringRule.RULE_CODE), eq("windowHours"), any(Integer.class)))
                .thenReturn(24);
        lenient()
                .when(ruleConfigService.integer(eq(StructuringRule.RULE_CODE), eq("minCount"), any(Integer.class)))
                .thenReturn(3);
    }

    @Test
    void triggersOnThreeQualifyingTransactionsWithinWindow() {
        Instant now = Instant.now();
        Transaction t1 = RuleTestFixtures.transaction(account, "DEBIT", "9200.00", now.minusSeconds(3 * 3600));
        Transaction t2 = RuleTestFixtures.transaction(account, "DEBIT", "9500.00", now.minusSeconds(2 * 3600));
        Transaction t3 = RuleTestFixtures.transaction(account, "DEBIT", "9800.00", now);
        when(transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getId()), any(), any()))
                .thenReturn(List.of(t1, t2, t3));
        when(ruleConfigService.weight(StructuringRule.RULE_CODE)).thenReturn(45);

        var result = rule.evaluate(t3);

        assertThat(result).isPresent();
        assertThat(result.get().riskContribution()).isEqualTo(45);
        assertThat(result.get().evidenceTransactionIds()).hasSize(3);
    }

    @Test
    void doesNotTriggerWithOnlyTwoQualifyingTransactions() {
        Instant now = Instant.now();
        Transaction t1 = RuleTestFixtures.transaction(account, "DEBIT", "9200.00", now.minusSeconds(3600));
        Transaction t2 = RuleTestFixtures.transaction(account, "DEBIT", "9500.00", now);
        when(transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getId()), any(), any()))
                .thenReturn(List.of(t1, t2));

        assertThat(rule.evaluate(t2)).isEmpty();
    }

    @Test
    void doesNotTriggerWhenAmountIsOutsideTheBand() {
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "5000.00", Instant.now());

        assertThat(rule.evaluate(transaction)).isEmpty();
    }
}
