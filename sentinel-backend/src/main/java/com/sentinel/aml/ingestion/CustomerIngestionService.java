package com.sentinel.aml.ingestion;

import static com.sentinel.aml.ingestion.CsvRowSupport.optional;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalBoolean;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalDate;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalDecimal;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalInt;
import static com.sentinel.aml.ingestion.CsvRowSupport.optionalOrDefault;
import static com.sentinel.aml.ingestion.CsvRowSupport.required;

import com.sentinel.aml.domain.Customer;
import com.sentinel.aml.domain.IngestionError;
import com.sentinel.aml.repository.CustomerRepository;
import com.sentinel.aml.repository.IngestionErrorRepository;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

@Service
public class CustomerIngestionService {

    private final CustomerRepository customerRepository;
    private final IngestionErrorRepository ingestionErrorRepository;

    public CustomerIngestionService(
            CustomerRepository customerRepository, IngestionErrorRepository ingestionErrorRepository) {
        this.customerRepository = customerRepository;
        this.ingestionErrorRepository = ingestionErrorRepository;
    }

    public IngestionSummary ingest(Reader csvReader) throws IOException {
        long start = System.currentTimeMillis();
        int accepted = 0;
        List<String> errors = new ArrayList<>();

        try (CSVParser parser =
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(csvReader)) {
            for (CSVRecord record : parser) {
                try {
                    Customer customer = mapRow(record);
                    if (customerRepository.existsByCustomerId(customer.getCustomerId())) {
                        throw new IllegalArgumentException("Duplicate customer_id: " + customer.getCustomerId());
                    }
                    customerRepository.save(customer);
                    accepted++;
                } catch (Exception ex) {
                    errors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                    ingestionErrorRepository.save(new IngestionError("CUSTOMER", record.toString(), ex.getMessage()));
                }
            }
        }

        return new IngestionSummary(accepted, errors.size(), errors, System.currentTimeMillis() - start);
    }

    private Customer mapRow(CSVRecord row) {
        Customer customer = new Customer();
        customer.setCustomerId(required(row, "customer_id"));
        customer.setFirstName(required(row, "first_name"));
        customer.setLastName(required(row, "last_name"));
        customer.setGender(optional(row, "gender"));
        customer.setDateOfBirth(optionalDate(row, "date_of_birth"));
        customer.setEmail(optional(row, "email"));
        customer.setPhoneNumber(optional(row, "phone_number"));
        customer.setCity(optional(row, "city"));
        customer.setState(optional(row, "state"));
        customer.setCountry(optional(row, "country"));
        customer.setPostalCode(optional(row, "postal_code"));
        customer.setOccupation(optional(row, "occupation"));
        customer.setAnnualIncome(optionalDecimal(row, "annual_income"));
        customer.setMaritalStatus(optional(row, "marital_status"));
        customer.setEducationLevel(optional(row, "education_level"));
        customer.setEmploymentStatus(optional(row, "employment_status"));
        customer.setCustomerSince(optionalDate(row, "customer_since"));
        customer.setCustomerSegment(optional(row, "customer_segment"));
        customer.setKycStatus(optionalOrDefault(row, "kyc_status", "PENDING"));
        customer.setRiskRating(optionalOrDefault(row, "risk_rating", "LOW"));
        customer.setPoliticallyExposed(optionalBoolean(row, "is_politically_exposed"));
        customer.setPreferredChannel(optional(row, "preferred_channel"));
        customer.setEmailVerified(optionalBoolean(row, "email_verified"));
        customer.setPhoneVerified(optionalBoolean(row, "phone_verified"));
        customer.setNumComplaintsLastYear(optionalInt(row, "num_complaints_last_year"));
        return customer;
    }
}
