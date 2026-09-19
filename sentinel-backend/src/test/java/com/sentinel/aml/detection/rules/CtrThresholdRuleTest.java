package com.sentinel.aml.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CtrThresholdRuleTest {

    @Mock private RuleConfigService ruleConfigService;

    private CtrThresholdRule rule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new CtrThresholdRule(ruleConfigService);
        account = RuleTestFixtures.account(1);
        when(ruleConfigService.bigDecimal(CtrThresholdRule.RULE_CODE, "minAmountBase", new BigDecimal("10000")))
                .thenReturn(new BigDecimal("10000"));
    }

    @Test
    void triggersAtExactlyTheThreshold() {
        when(ruleConfigService.weight(CtrThresholdRule.RULE_CODE)).thenReturn(40);
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "10000.00", Instant.now());

        var result = rule.evaluate(transaction);

        assertThat(result).isPresent();
        assertThat(result.get().riskContribution()).isEqualTo(40);
        assertThat(result.get().evidenceTransactionIds()).containsExactly(transaction.getId().toString());
    }

    @Test
    void triggersAboveTheThreshold() {
        when(ruleConfigService.weight(CtrThresholdRule.RULE_CODE)).thenReturn(40);
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "12500.00", Instant.now());

        assertThat(rule.evaluate(transaction)).isPresent();
    }

    @Test
    void doesNotTriggerJustBelowTheThreshold() {
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "9999.99", Instant.now());

        assertThat(rule.evaluate(transaction)).isEmpty();
    }
}
