package com.sentinel.aml.directory;

import com.sentinel.aml.common.CurrentActor;
import com.sentinel.aml.common.NotFoundException;
import com.sentinel.aml.common.PiiMasker;
import com.sentinel.aml.domain.Customer;
import com.sentinel.aml.dto.CustomerDetailDto;
import com.sentinel.aml.dto.CustomerSummaryDto;
import com.sentinel.aml.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * PII masking (business rule 8) is enforced here, not in the frontend: list views are always
 * masked; detail views are only unmasked for ADMIN/COMPLIANCE_ANALYST.
 */
@Service
public class CustomerQueryService {

    private final CustomerRepository customerRepository;

    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Page<CustomerSummaryDto> list(Pageable pageable) {
        return customerRepository.findAllByOrderByIdAsc(pageable).map(this::toSummary);
    }

    public CustomerDetailDto get(String customerId) {
        Customer customer =
                customerRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(() -> new NotFoundException("No such customer: " + customerId));
        boolean authorizedForFullPii = isAuthorizedForFullPii();
        return new CustomerDetailDto(
                customer.getId(),
                customer.getCustomerId(),
                authorizedForFullPii ? customer.getFirstName() : PiiMasker.maskName(customer.getFirstName()),
                authorizedForFullPii ? customer.getLastName() : PiiMasker.maskName(customer.getLastName()),
                authorizedForFullPii ? customer.getEmail() : null,
                authorizedForFullPii ? customer.getPhoneNumber() : null,
                customer.getDateOfBirth(),
                customer.getCity(),
                customer.getState(),
                customer.getCountry(),
                customer.getOccupation(),
                customer.getAnnualIncome(),
                customer.getKycStatus(),
                customer.getRiskRating(),
                customer.isPoliticallyExposed(),
                customer.getCustomerSegment(),
                !authorizedForFullPii);
    }

    private CustomerSummaryDto toSummary(Customer customer) {
        String displayName = PiiMasker.maskName(customer.getFirstName()) + " " + PiiMasker.maskName(customer.getLastName());
        return new CustomerSummaryDto(
                customer.getId(),
                customer.getCustomerId(),
                displayName,
                customer.getRiskRating(),
                customer.getKycStatus(),
                customer.getCountry(),
                customer.getCustomerSegment(),
                customer.isPoliticallyExposed());
    }

    private boolean isAuthorizedForFullPii() {
        String role = CurrentActor.fromSecurityContext().role();
        return role.equals("ADMIN") || role.equals("COMPLIANCE_ANALYST");
    }
}
