package com.sentinel.aml.detection;

import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.domain.TransactionDirection;
import com.sentinel.aml.repository.TransactionRepository;
import com.sentinel.aml.detection.rules.RoundNumberPatternRule;
import com.sentinel.aml.detection.rules.StructuringRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Builds the {@link TransactionFeatures} vector the anomaly model scores. Reuses the same
 * windows/config the rule engine already queries so the two signals reason about the same data.
 */
@Service
public class FeatureExtractionService {

    private final TransactionRepository transactionRepository;
    private final RuleConfigService ruleConfigService;
    private final HighRiskJurisdictionService highRiskJurisdictionService;

    public FeatureExtractionService(
            TransactionRepository transactionRepository,
            RuleConfigService ruleConfigService,
            HighRiskJurisdictionService highRiskJurisdictionService) {
        this.transactionRepository = transactionRepository;
        this.ruleConfigService = ruleConfigService;
        this.highRiskJurisdictionService = highRiskJurisdictionService;
    }

    public TransactionFeatures extract(Transaction transaction) {
        Instant now = transaction.getTxnTimestamp();
        Long accountId = transaction.getAccount().getId();
        Long customerId = transaction.getAccount().getCustomer().getId();

        List<Transaction> last24h =
                transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        accountId, now.minusSeconds(86400), now);
        List<Transaction> last48h =
                transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        accountId, now.minusSeconds(2 * 86400L), now);
        List<Transaction> last90d =
                transactionRepository.findByAccount_Customer_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        customerId, now.minusSeconds(90L * 86400L), now);

        double sumLast24h =
                last24h.stream().mapToDouble(t -> t.getAmountBaseCurrency().doubleValue()).sum();

        double rollingAvgAmount90d =
                last90d.isEmpty()
                        ? transaction.getAmountBaseCurrency().doubleValue()
                        : last90d.stream().mapToDouble(t -> t.getAmountBaseCurrency().doubleValue()).average().orElse(0);
        double rollingAvgCount90d = last90d.size() / 90.0;

        double pctWithdrawnWithin48h = maxOutflowRatio(last48h);

        Set<String> counterparties = new HashSet<>();
        for (Transaction t : last24h) {
            if (t.getCounterpartyAccount() != null) {
                counterparties.add(t.getCounterpartyAccount());
            } else if (t.getCounterpartyName() != null) {
                counterparties.add(t.getCounterpartyName());
            }
        }

        boolean highRisk =
                highRiskJurisdictionService.isHighRisk(transaction.getJurisdiction())
                        || highRiskJurisdictionService.isHighRisk(transaction.getCounterpartyCountry());

        BigDecimal roundToNearest =
                ruleConfigService.bigDecimal(RoundNumberPatternRule.RULE_CODE, "roundToNearest", new BigDecimal("1000"));
        boolean roundNumber =
                roundToNearest.signum() > 0 && transaction.getAmount().remainder(roundToNearest).signum() == 0;

        BigDecimal structMin =
                ruleConfigService.bigDecimal(StructuringRule.RULE_CODE, "minAmountBase", new BigDecimal("9000"));
        BigDecimal structMax =
                ruleConfigService.bigDecimal(StructuringRule.RULE_CODE, "maxAmountBase", new BigDecimal("9999"));
        boolean justBelowThreshold =
                transaction.getAmountBaseCurrency().compareTo(structMin) >= 0
                        && transaction.getAmountBaseCurrency().compareTo(structMax) <= 0;

        long accountAgeDays =
                java.time.temporal.ChronoUnit.DAYS.between(
                        transaction.getAccount().getOpenDate(), now.atZone(ZoneOffset.UTC).toLocalDate());

        return TransactionFeatures.builder()
                .amount(transaction.getAmountBaseCurrency().doubleValue())
                .rollingAvgAmount90d(rollingAvgAmount90d)
                .rollingAvgCount90d(rollingAvgCount90d)
                .txnCountLast24h(last24h.size())
                .sumAmountLast24h(sumLast24h)
                .pctOfDepositWithdrawnWithin48h(pctWithdrawnWithin48h)
                .isHighRiskJurisdiction(highRisk ? 1.0 : 0.0)
                .isRoundNumberAmount(roundNumber ? 1.0 : 0.0)
                .isJustBelowReportingThreshold(justBelowThreshold ? 1.0 : 0.0)
                .hourOfDay(now.atZone(ZoneOffset.UTC).getHour())
                .distinctCounterpartiesLast24h(counterparties.size())
                .accountAgeDays(Math.max(0, accountAgeDays))
                .build();
    }

    /** Largest fraction of any single recent deposit that's already been moved back out. */
    private double maxOutflowRatio(List<Transaction> window) {
        double maxRatio = 0.0;
        for (Transaction deposit : window) {
            if (deposit.getDirection() != TransactionDirection.CREDIT || deposit.getAmountBaseCurrency().signum() <= 0) {
                continue;
            }
            BigDecimal outflowSince =
                    window.stream()
                            .filter(t -> t.getDirection() == TransactionDirection.DEBIT)
                            .filter(t -> !t.getTxnTimestamp().isBefore(deposit.getTxnTimestamp()))
                            .map(Transaction::getAmountBaseCurrency)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            double ratio =
                    outflowSince.divide(deposit.getAmountBaseCurrency(), 4, RoundingMode.HALF_UP).doubleValue();
            maxRatio = Math.max(maxRatio, Math.min(1.0, ratio));
        }
        return maxRatio;
    }
}
