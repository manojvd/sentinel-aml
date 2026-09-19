package com.sentinel.aml.repository;

import com.sentinel.aml.domain.HighRiskJurisdiction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HighRiskJurisdictionRepository extends JpaRepository<HighRiskJurisdiction, String> {

    List<HighRiskJurisdiction> findByActiveTrue();
}
