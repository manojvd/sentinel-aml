-- Reference/config data. All of this is editable at runtime via the admin API
-- (ExchangeRateController, HighRiskJurisdictionController, DetectionRuleController)
-- without a redeploy -- this seed is just a sane starting point.

INSERT INTO exchange_rates (currency_code, rate_to_base) VALUES
    ('INR', 1.0),
    ('USD', 91.50),
    ('EUR', 99.20),
    ('GBP', 116.40),
    ('AED', 24.90),
    ('SGD', 68.50);

INSERT INTO high_risk_jurisdictions (country_code, reason) VALUES
    ('IR', 'FATF high-risk jurisdiction subject to a call for action'),
    ('KP', 'FATF high-risk jurisdiction subject to a call for action'),
    ('MM', 'FATF jurisdiction under increased monitoring'),
    ('SY', 'Comprehensive international sanctions regime'),
    ('AF', 'Elevated ML/TF risk -- limited correspondent banking oversight');

INSERT INTO detection_rules (rule_code, name, description, weight, config) VALUES
    ('CTR_THRESHOLD', 'CTR-style large transaction',
     'Any single transaction at or above the reporting threshold (base-currency equivalent).',
     40, '{"minAmountBase": 10000}'),

    ('STRUCTURING', 'Structuring / smurfing',
     '3+ transactions from the same account within a rolling window, each just under the reporting threshold.',
     45, '{"minAmountBase": 9000, "maxAmountBase": 9999, "windowHours": 24, "minCount": 3}'),

    ('RAPID_MOVEMENT', 'Rapid movement of funds',
     'A deposit where a large share of the value is moved back out again within a short window (layering).',
     40, '{"outflowPct": 80, "windowHours": 48}'),

    ('HIGH_RISK_JURISDICTION', 'High-risk jurisdiction transfer',
     'Transaction involving a counterparty or jurisdiction on the configurable high-risk/sanctions list.',
     50, '{}'),

    ('BEHAVIORAL_DEVIATION', 'Behavioral deviation',
     'A customer''s daily transaction volume/value exceeds a multiple of their rolling historical average.',
     30, '{"deviationMultiplier": 3, "baselineDays": 90}'),

    ('ROUND_NUMBER', 'Round-number pattern',
     'Repeated suspiciously round-number transactions from the same account within a window.',
     20, '{"roundToNearest": 1000, "windowHours": 24, "minCount": 3}');
