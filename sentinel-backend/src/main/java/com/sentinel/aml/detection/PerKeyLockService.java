package com.sentinel.aml.detection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

/**
 * Serializes alert upserts per customer so two concurrent transaction streams for the same
 * customer can't race and create duplicate/lost alerts. Backed additionally by a DB partial
 * unique index ({@code ux_alerts_open_dedup}) as a second line of defense.
 */
@Service
public class PerKeyLockService {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock lockFor(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }
}
