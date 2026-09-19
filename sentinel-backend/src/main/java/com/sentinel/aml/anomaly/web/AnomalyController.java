package com.sentinel.aml.anomaly.web;

import com.sentinel.aml.anomaly.AnomalyScoringService;
import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ML anomaly scoring endpoints, standing in for the rule engine's eventual internal
 * call to {@link AnomalyScoringService} while that engine is still being built. Also
 * useful standalone for the demo dashboard to show "why the model flagged this."
 */
@RestController
@RequestMapping("/api/v1/anomaly")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyScoringService anomalyScoringService;

    @PostMapping("/score")
    public ResponseEntity<AnomalyScoreResult> score(@Valid @RequestBody TransactionFeatures features) {
        return ResponseEntity.ok(anomalyScoringService.score(features));
    }

    @PostMapping("/train")
    public ResponseEntity<Map<String, Object>> train(@RequestBody @NotEmpty List<@Valid TransactionFeatures> history) {
        anomalyScoringService.train(history);
        return ResponseEntity.ok(statusBody());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(statusBody());
    }

    private Map<String, Object> statusBody() {
        return Map.of(
                "algorithm", anomalyScoringService.algorithm(),
                "trained", anomalyScoringService.isTrained(),
                "trainingSize", anomalyScoringService.trainingSize());
    }
}
