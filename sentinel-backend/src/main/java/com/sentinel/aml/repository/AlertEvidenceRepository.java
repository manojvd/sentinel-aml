package com.sentinel.aml.repository;

import com.sentinel.aml.domain.Alert;
import com.sentinel.aml.domain.AlertEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertEvidenceRepository extends JpaRepository<AlertEvidence, Long> {

    List<AlertEvidence> findByAlertOrderByCreatedAtAsc(Alert alert);
}
