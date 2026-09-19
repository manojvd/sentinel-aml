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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RapidMovementRuleTest {

    @Mock private RuleConfigService ruleConfigService;
    @Mock private TransactionRepository transactionRepository;

    private RapidMovementRule rule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new RapidMovementRule(ruleConfigService, transactionRepository);
        account = RuleTestFixtures.account(1);
        lenient()
                .when(ruleConfigService.integer(eq(RapidMovementRule.RULE_CODE), eq("outflowPct"), any(Integer.class)))
                .thenReturn(80);
        lenient()
                .when(ruleConfigService.integer(eq(RapidMovementRule.RULE_CODE), eq("windowHours"), any(Integer.class)))
                .thenReturn(48);
    }

    @Test
    void triggersWhenEightyPercentOfADepositMovesOutWithin48h() {
        Instant now = Instant.now();
        Transaction deposit = RuleTestFixtures.transaction(account, "CREDIT", "50000.00", now.minusSeconds(20 * 3600));
        Transaction out1 = RuleTestFixtures.transaction(account, "DEBIT", "25000.00", now.minusSeconds(10 * 3600));
        Transaction out2 = RuleTestFixtures.transaction(account, "DEBIT", "20000.00", now);
        when(transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getId()), any(), any()))
                .thenReturn(List.of(deposit, out1, out2));
        when(ruleConfigService.weight(RapidMovementRule.RULE_CODE)).thenReturn(40);

        var result = rule.evaluate(out2);

        assertThat(result).isPresent();
        assertThat(result.get().evidenceTransactionIds()).contains(deposit.getId().toString(), out1.getId().toString(), out2.getId().toString());
    }

    @Test
    void doesNotTriggerBelowTheOutflowThreshold() {
        Instant now = Instant.now();
        Transaction deposit = RuleTestFixtures.transaction(account, "CREDIT", "50000.00", now.minusSeconds(20 * 3600));
        Transaction out1 = RuleTestFixtures.transaction(account, "DEBIT", "10000.00", now);
        when(transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        eq(account.getId()), any(), any()))
                .thenReturn(List.of(deposit, out1));

        assertThat(rule.evaluate(out1)).isEmpty();
    }

    @Test
    void ignoresCreditTransactionsEntirely() {
        Transaction deposit = RuleTestFixtures.transaction(account, "CREDIT", "5000.00", Instant.now());

        assertThat(rule.evaluate(deposit)).isEmpty();
    }
}
