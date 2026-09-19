package com.sentinel.aml.repository;

import com.sentinel.aml.domain.Account;
import com.sentinel.aml.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountId(String accountId);

    boolean existsByAccountId(String accountId);

    List<Account> findByCustomer(Customer customer);

    List<Account> findByCustomerId(Long customerId);
}
