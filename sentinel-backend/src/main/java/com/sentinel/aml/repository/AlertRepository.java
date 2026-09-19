package com.sentinel.aml.repository;

import com.sentinel.aml.domain.Alert;
import com.sentinel.aml.domain.AlertStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, java.util.UUID> {

    Optional<Alert> findByDedupKeyAndStatusIn(String dedupKey, java.util.Collection<AlertStatus> statuses);

    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    Page<Alert> findAllByOrderByRiskScoreDescCreatedAtDesc(Pageable pageable);

    Page<Alert> findByStatusOrderByRiskScoreDescCreatedAtDesc(AlertStatus status, Pageable pageable);
}
