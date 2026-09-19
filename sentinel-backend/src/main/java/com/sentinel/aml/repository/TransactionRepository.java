package com.sentinel.aml.repository;

import com.sentinel.aml.domain.Transaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
            Long accountId, Instant from, Instant to);

    List<Transaction> findByAccount_Customer_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
            Long customerId, Instant from, Instant to);

    Page<Transaction> findByAccount_IdOrderByTxnTimestampDesc(Long accountId, Pageable pageable);
}
