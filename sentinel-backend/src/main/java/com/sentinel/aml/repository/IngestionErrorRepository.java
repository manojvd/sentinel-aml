package com.sentinel.aml.repository;

import com.sentinel.aml.domain.IngestionError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionErrorRepository extends JpaRepository<IngestionError, Long> {
}
