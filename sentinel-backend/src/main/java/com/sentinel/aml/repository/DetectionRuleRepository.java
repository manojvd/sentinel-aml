package com.sentinel.aml.repository;

import com.sentinel.aml.domain.DetectionRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionRuleRepository extends JpaRepository<DetectionRule, String> {

    List<DetectionRule> findByEnabledTrue();
}
