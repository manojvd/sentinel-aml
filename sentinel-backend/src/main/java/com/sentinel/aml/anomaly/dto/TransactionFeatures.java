package com.sentinel.aml.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Numeric feature vector describing a single transaction plus the account/customer
 * context around it, at the moment the transaction is evaluated.
 *
 * <p>This is the integration seam between the anomaly-scoring module and the rest of
 * Sentinel: once the ingestion/rule-engine layer has real {@code Customer}/{@code Account}/
 * {@code Transaction} entities, a feature-extraction step there should populate this DTO
 * (e.g. from rolling aggregates already needed for the Behavioral Deviation and Rapid
 * Movement rules) and hand it to {@link com.sentinel.aml.anomaly.AnomalyScoringService}.
 * Field order/semantics here intentionally mirror the AML typologies in the spec so the
 * model and the rule engine reason about the same signals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFeatures {

    /** Names of {@link #toVector()} entries, in order — for logging/explainability. */
    public static final String[] FEATURE_NAMES = {
        "amount",
        "rollingAvgAmount90d",
        "rollingAvgCount90d",
        "txnCountLast24h",
        "sumAmountLast24h",
        "pctOfDepositWithdrawnWithin48h",
        "isHighRiskJurisdiction",
        "isRoundNumberAmount",
        "isJustBelowReportingThreshold",
        "hourOfDay",
        "distinctCounterpartiesLast24h",
        "accountAgeDays"
    };

    /** Transaction amount, normalized to the base currency. */
    private double amount;

    /** Customer's 90-day rolling average transaction amount (behavioral baseline). */
    private double rollingAvgAmount90d;

    /** Customer's 90-day rolling average daily transaction count (behavioral baseline). */
    private double rollingAvgCount90d;

    /** Number of transactions on this account in the last 24 hours (structuring signal). */
    private double txnCountLast24h;

    /** Sum of transaction amounts on this account in the last 24 hours. */
    private double sumAmountLast24h;

    /** Fraction (0-1) of a recent deposit already moved back out within 48h (layering signal). */
    private double pctOfDepositWithdrawnWithin48h;

    /** 1.0 if counterparty/jurisdiction is on the high-risk/sanctions list, else 0.0. */
    private double isHighRiskJurisdiction;

    /** 1.0 if the amount is a suspiciously round number, else 0.0. */
    private double isRoundNumberAmount;

    /** 1.0 if amount falls just under a reporting threshold (e.g. $9,000-$9,999), else 0.0. */
    private double isJustBelowReportingThreshold;

    /** Hour of day (0-23) the transaction occurred, local to the account's branch/timezone. */
    private double hourOfDay;

    /** Distinct counterparties this account has transacted with in the last 24 hours. */
    private double distinctCounterpartiesLast24h;

    /** Age of the account in days at the time of the transaction. */
    private double accountAgeDays;

    /** Flattens this feature set into the fixed-order vector the ML models consume. */
    public double[] toVector() {
        return new double[] {
            amount,
            rollingAvgAmount90d,
            rollingAvgCount90d,
            txnCountLast24h,
            sumAmountLast24h,
            pctOfDepositWithdrawnWithin48h,
            isHighRiskJurisdiction,
            isRoundNumberAmount,
            isJustBelowReportingThreshold,
            hourOfDay,
            distinctCounterpartiesLast24h,
            accountAgeDays
        };
    }
}
