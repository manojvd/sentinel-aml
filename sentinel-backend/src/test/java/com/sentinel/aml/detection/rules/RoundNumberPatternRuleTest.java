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
class RoundNumberPatternRuleTest {

    @Mock private RuleConfigService ruleConfigService;
    @Mock private TransactionRepository transactionRepository;

    private RoundNumberPatternRule rule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new RoundNumberPatternRule(ruleConfigService, transactionRepository);
        account = RuleTestFixtures.account(1);
        lenient()
                .when(ruleConfigService.bigDecimal(eq(RoundNumberPatternRule.RULE_CODE), eq("roundToNearest"), any()))
                .thenReturn(new BigDecimal("1000"));
        lenient()
                .when(ruleConfigService.integer(eq(RoundNumberPatternRule.RULE_CODE), eq("windowHours"), any(Integer.class)))
                .thenReturn(24);
        lenient()
                .when(ruleConfigService.integer(eq(RoundNumberPatternRule.RULE_CODE), eq("minCount"), any(Integer.class)))
                .thenReturn(3);
    }

    @Test
    void triggersOnThreeRoundAmountsWithinWindow() {
        Instant now = Instant.now();
        Transaction t1 = RuleTestFixtures.transaction(account, "DEBIT", "5000.00", now.minusSeconds(6 * 3600));
        Transaction t2 = RuleTestFixtures.transaction(account, "DEBIT", "10000.00", now.minusSeconds(4 * 3600));
        Transaction t3 = RuleTestFixtures.transaction(account, "DEBIT", "15000.00", now);
        when(transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getId()), any(), any()))
                .thenReturn(List.of(t1, t2, t3));
        when(ruleConfigService.weight(RoundNumberPatternRule.RULE_CODE)).thenReturn(20);

        var result = rule.evaluate(t3);

        assertThat(result).isPresent();
        assertThat(result.get().evidenceTransactionIds()).hasSize(3);
    }

    @Test
    void doesNotTriggerForNonRoundAmount() {
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "4237.50", Instant.now());

        assertThat(rule.evaluate(transaction)).isEmpty();
    }

    @Test
    void doesNotTriggerWithFewerThanMinCountRoundAmounts() {
        Instant now = Instant.now();
        Transaction t1 = RuleTestFixtures.transaction(account, "DEBIT", "5000.00", now.minusSeconds(3600));
        Transaction t2 = RuleTestFixtures.transaction(account, "DEBIT", "10000.00", now);
        when(transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getId()), any(), any()))
                .thenReturn(List.of(t1, t2));

        assertThat(rule.evaluate(t2)).isEmpty();
    }
}
