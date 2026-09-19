package com.sentinel.aml.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sentinel.aml.detection.HighRiskJurisdictionService;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.Transaction;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HighRiskJurisdictionRuleTest {

    @Mock private RuleConfigService ruleConfigService;
    @Mock private HighRiskJurisdictionService highRiskJurisdictionService;

    private HighRiskJurisdictionRule rule;
    private Account account;

    @BeforeEach
    void setUp() {
        rule = new HighRiskJurisdictionRule(ruleConfigService, highRiskJurisdictionService);
        account = RuleTestFixtures.account(1);
    }

    @Test
    void triggersRegardlessOfAmountWhenJurisdictionIsHighRisk() {
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "10.00", Instant.now());
        transaction.setJurisdiction("IR");
        lenient().when(highRiskJurisdictionService.isHighRisk("IR")).thenReturn(true);
        when(ruleConfigService.weight(HighRiskJurisdictionRule.RULE_CODE)).thenReturn(50);

        var result = rule.evaluate(transaction);

        assertThat(result).isPresent();
        assertThat(result.get().explanation()).contains("IR");
    }

    @Test
    void triggersWhenOnlyCounterpartyCountryIsHighRisk() {
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "500.00", Instant.now());
        transaction.setCounterpartyCountry("KP");
        lenient().when(highRiskJurisdictionService.isHighRisk("KP")).thenReturn(true);
        when(ruleConfigService.weight(HighRiskJurisdictionRule.RULE_CODE)).thenReturn(50);

        assertThat(rule.evaluate(transaction)).isPresent();
    }

    @Test
    void doesNotTriggerForOrdinaryJurisdictions() {
        Transaction transaction = RuleTestFixtures.transaction(account, "DEBIT", "500.00", Instant.now());
        transaction.setJurisdiction("IN");
        lenient().when(highRiskJurisdictionService.isHighRisk("IN")).thenReturn(false);

        assertThat(rule.evaluate(transaction)).isEmpty();
    }
}
