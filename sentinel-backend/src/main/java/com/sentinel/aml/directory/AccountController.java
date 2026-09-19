package com.sentinel.aml.directory;

import com.sentinel.aml.dto.AccountSummaryDto;
import com.sentinel.aml.dto.TransactionDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account detail and per-account transaction timeline")
public class AccountController {

    private final AccountQueryService accountQueryService;

    public AccountController(AccountQueryService accountQueryService) {
        this.accountQueryService = accountQueryService;
    }

    @GetMapping("/{accountId}")
    public AccountSummaryDto get(@PathVariable String accountId) {
        return accountQueryService.get(accountId);
    }

    @GetMapping("/{accountId}/transactions")
    public Page<TransactionDto> timeline(@PathVariable String accountId, Pageable pageable) {
        return accountQueryService.timeline(accountId, pageable);
    }
}
