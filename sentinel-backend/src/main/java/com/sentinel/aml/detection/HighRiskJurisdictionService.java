package com.sentinel.aml.detection;

import com.sentinel.aml.repository.HighRiskJurisdictionRepository;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HighRiskJurisdictionService {

    private static final long CACHE_TTL_SECONDS = 30;

    private final HighRiskJurisdictionRepository repository;
    private final AtomicReference<Set<String>> cache = new AtomicReference<>(Set.of());
    private final AtomicReference<Instant> lastRefresh = new AtomicReference<>(Instant.EPOCH);

    public HighRiskJurisdictionService(HighRiskJurisdictionRepository repository) {
        this.repository = repository;
    }

    public void invalidate() {
        lastRefresh.set(Instant.EPOCH);
    }

    private Set<String> activeCodes() {
        if (Instant.now().isAfter(lastRefresh.get().plusSeconds(CACHE_TTL_SECONDS))) {
            synchronized (this) {
                if (Instant.now().isAfter(lastRefresh.get().plusSeconds(CACHE_TTL_SECONDS))) {
                    cache.set(
                            repository.findByActiveTrue().stream()
                                    .map(j -> j.getCountryCode().toUpperCase())
                                    .collect(Collectors.toUnmodifiableSet()));
                    lastRefresh.set(Instant.now());
                }
            }
        }
        return cache.get();
    }

    public boolean isHighRisk(String countryCode) {
        return countryCode != null && activeCodes().contains(countryCode.toUpperCase());
    }
}
