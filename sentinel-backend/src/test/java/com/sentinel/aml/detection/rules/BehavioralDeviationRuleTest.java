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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BehavioralDeviationRuleTest {

    @Mock private RuleConfigService ruleConfigService;
    @Mock private TransactionRepository transactionRepository;

    private BehavioralDeviationRule rule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new BehavioralDeviationRule(ruleConfigService, transactionRepository);
        account = RuleTestFixtures.account(1);
        lenient()
                .when(ruleConfigService.integer(eq(BehavioralDeviationRule.RULE_CODE), eq("deviationMultiplier"), any(Integer.class)))
                .thenReturn(3);
        lenient()
                .when(ruleConfigService.integer(eq(BehavioralDeviationRule.RULE_CODE), eq("baselineDays"), any(Integer.class)))
                .thenReturn(90);
    }

    @Test
    void triggersWhenTodayExceedsThreeTimesTheRollingAverage() {
        Instant now = Instant.now();
        // 90 days of a ~100/day baseline (9000 total / 90 days = 100/day average).
        List<Transaction> baseline = new ArrayList<>();
        for (int day = 1; day <= 90; day++) {
            baseline.add(RuleTestFixtures.transaction(account, "DEBIT", "100.00", now.minusSeconds(day * 86400L)));
        }
        Transaction today = RuleTestFixtures.transaction(account, "DEBIT", "500.00", now);
        List<Transaction> withToday = new ArrayList<>(baseline);
        withToday.add(today);

        when(transactionRepository.findByAccount_Customer_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getCustomer().getId()), any(), any()))
                .thenReturn(withToday);
        when(ruleConfigService.weight(BehavioralDeviationRule.RULE_CODE)).thenReturn(30);

        var result = rule.evaluate(today);

        assertThat(result).isPresent();
        assertThat(result.get().evidenceTransactionIds()).containsExactly(today.getId().toString());
    }

    @Test
    void doesNotTriggerWhenTodayIsWithinNormalRange() {
        Instant now = Instant.now();
        List<Transaction> baseline = new ArrayList<>();
        for (int day = 1; day <= 90; day++) {
            baseline.add(RuleTestFixtures.transaction(account, "DEBIT", "100.00", now.minusSeconds(day * 86400L)));
        }
        Transaction today = RuleTestFixtures.transaction(account, "DEBIT", "110.00", now);
        List<Transaction> withToday = new ArrayList<>(baseline);
        withToday.add(today);

        when(transactionRepository.findByAccount_Customer_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getCustomer().getId()), any(), any()))
                .thenReturn(withToday);

        assertThat(rule.evaluate(today)).isEmpty();
    }

    @Test
    void doesNotTriggerWithNoBaselineHistory() {
        Transaction today = RuleTestFixtures.transaction(account, "DEBIT", "5000.00", Instant.now());
        when(transactionRepository.findByAccount_Customer_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getCustomer().getId()), any(), any()))
                .thenReturn(List.of(today));

        assertThat(rule.evaluate(today)).isEmpty();
    }
}
