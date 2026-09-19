package com.sentinel.aml.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, unique = true)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "account_status", nullable = false)
    private String accountStatus = "ACTIVE";

    @Column(nullable = false)
    private String currency;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "branch_city")
    private String branchCity;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "avg_monthly_balance_6m")
    private BigDecimal avgMonthlyBalance6m;

    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    @Column(name = "credit_utilization_pct")
    private BigDecimal creditUtilizationPct;

    @Column(name = "overdraft_enabled", nullable = false)
    private boolean overdraftEnabled;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "is_joint_account", nullable = false)
    private boolean jointAccount;

    @Column(name = "mobile_banking_enrolled", nullable = false)
    private boolean mobileBankingEnrolled;

    @Column(name = "avg_monthly_txn_count")
    private Integer avgMonthlyTxnCount;

    @Column(name = "account_tier")
    private String accountTier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
