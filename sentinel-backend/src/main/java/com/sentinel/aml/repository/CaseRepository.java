package com.sentinel.aml.repository;

import com.sentinel.aml.domain.Case;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<Case, UUID> {

    Page<Case> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
