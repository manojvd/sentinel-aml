package com.sentinel.aml.directory;

import com.sentinel.aml.dto.AccountSummaryDto;
import com.sentinel.aml.dto.CustomerDetailDto;
import com.sentinel.aml.dto.CustomerSummaryDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer directory -- PII masked per role, see business rule 8")
public class CustomerController {

    private final CustomerQueryService customerQueryService;
    private final AccountQueryService accountQueryService;

    public CustomerController(CustomerQueryService customerQueryService, AccountQueryService accountQueryService) {
        this.customerQueryService = customerQueryService;
        this.accountQueryService = accountQueryService;
    }

    @GetMapping
    public Page<CustomerSummaryDto> list(Pageable pageable) {
        return customerQueryService.list(pageable);
    }

    @GetMapping("/{customerId}")
    public CustomerDetailDto get(@PathVariable String customerId) {
        return customerQueryService.get(customerId);
    }

    @GetMapping("/{customerId}/accounts")
    public List<AccountSummaryDto> accounts(@PathVariable String customerId) {
        return accountQueryService.listForCustomer(customerId);
    }
}
