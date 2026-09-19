-- Pseudo "rule" row so ML-anomaly-sourced alert_evidence rows (rule_code = 'ANOMALY_ML') satisfy
-- the same FK as rule-engine evidence. Not evaluated by DetectionRule -- see RiskScoreCombiner.
INSERT INTO detection_rules (rule_code, name, description, weight, config) VALUES
    ('ANOMALY_ML', 'ML anomaly signal',
     'Statistical outlier flagged by the anomaly-scoring model (Isolation Forest / K-Means), blended with rule-engine results via RiskScoreCombiner.',
     0, '{}');
