package com.sentinel.aml.detection;

import com.sentinel.aml.domain.DetectionRule;
import com.sentinel.aml.repository.DetectionRuleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Short-TTL cache in front of the {@code detection_rules} table so the engine can evaluate
 * transactions at high throughput without hitting the DB on every row, while still picking up
 * threshold/weight/enabled changes made via the admin API without a redeploy.
 */
@Service
public class RuleConfigService {

    private final DetectionRuleRepository detectionRuleRepository;
    private final long cacheTtlSeconds;
    private final AtomicReference<Map<String, DetectionRule>> cache = new AtomicReference<>(Map.of());
    private final AtomicReference<Instant> lastRefresh = new AtomicReference<>(Instant.EPOCH);

    public RuleConfigService(
            DetectionRuleRepository detectionRuleRepository,
            @Value("${sentinel.detection.rule-config-cache-ttl-seconds}") long cacheTtlSeconds) {
        this.detectionRuleRepository = detectionRuleRepository;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public void invalidate() {
        lastRefresh.set(Instant.EPOCH);
    }

    private Map<String, DetectionRule> rules() {
        if (Instant.now().isAfter(lastRefresh.get().plusSeconds(cacheTtlSeconds))) {
            synchronized (this) {
                if (Instant.now().isAfter(lastRefresh.get().plusSeconds(cacheTtlSeconds))) {
                    Map<String, DetectionRule> fresh = new ConcurrentHashMap<>();
                    detectionRuleRepository.findAll().forEach(r -> fresh.put(r.getRuleCode(), r));
                    cache.set(fresh);
                    lastRefresh.set(Instant.now());
                }
            }
        }
        return cache.get();
    }

    public boolean isEnabled(String ruleCode) {
        DetectionRule rule = rules().get(ruleCode);
        return rule != null && rule.isEnabled();
    }

    public int weight(String ruleCode) {
        return Optional.ofNullable(rules().get(ruleCode)).map(DetectionRule::getWeight).orElse(0);
    }

    public Map<String, Object> config(String ruleCode) {
        return Optional.ofNullable(rules().get(ruleCode))
                .map(DetectionRule::getConfig)
                .orElse(Map.of());
    }

    public BigDecimal bigDecimal(String ruleCode, String key, BigDecimal defaultValue) {
        Object value = config(ruleCode).get(key);
        return value == null ? defaultValue : new BigDecimal(value.toString());
    }

    public int integer(String ruleCode, String key, int defaultValue) {
        Object value = config(ruleCode).get(key);
        return value == null ? defaultValue : new BigDecimal(value.toString()).intValue();
    }
}
