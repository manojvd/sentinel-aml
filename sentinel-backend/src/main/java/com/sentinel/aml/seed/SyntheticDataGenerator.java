package com.sentinel.aml.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Deterministic (fixed-seed) synthetic data so the demo dataset is reproducible. Deliberately
 * crafts one transaction sequence per AML typology so the seeded run always produces alerts to
 * show in the demo, on top of realistic benign noise to reach bulk-load scale.
 */
@Component
public class SyntheticDataGenerator {

    private static final String[] FIRST_NAMES = {
        "Aarav", "Vivaan", "Aditya", "Ananya", "Diya", "Ishaan", "Kavya", "Meera", "Neha", "Rohan",
        "Saanvi", "Tanvi", "Vikram", "Yash", "Zara", "Arjun", "Priya", "Karan", "Sneha", "Manish"
    };
    private static final String[] LAST_NAMES = {
        "Sharma", "Verma", "Iyer", "Reddy", "Nair", "Gupta", "Mehta", "Rao", "Chawla", "Bose",
        "Kapoor", "Joshi", "Malhotra", "Pillai", "Desai"
    };
    private static final String[] CITIES_STATES = {
        "Mumbai,Maharashtra", "Bengaluru,Karnataka", "Gurugram,Haryana", "Pune,Maharashtra",
        "Chennai,Tamil Nadu", "Hyderabad,Telangana", "Kolkata,West Bengal", "Ahmedabad,Gujarat"
    };
    private static final String[] OCCUPATIONS = {
        "Software Engineer", "Accountant", "Business Owner", "Teacher", "Doctor", "Consultant", "Trader"
    };
    private static final String[] CHANNELS = {"NEFT", "IMPS", "UPI", "RTGS", "CARD", "WIRE"};
    private static final String[] BENIGN_COUNTERPARTIES = {
        "Amazon Retail", "Local Grocers", "Electricity Board", "Landlord", "Employer Payroll", "Metro Card Recharge"
    };

    private final Random random = new Random(42);

    public List<String> generateCustomerIds(int count, int startIndex) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add("CUST_%05d".formatted(startIndex + i));
        }
        return ids;
    }

    public String generateCustomersCsv(int count, int startIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "customer_id,first_name,last_name,gender,date_of_birth,age,email,phone_number,city,state,country,"
                        + "postal_code,occupation,annual_income,marital_status,education_level,employment_status,"
                        + "customer_since,customer_segment,kyc_status,risk_rating,is_politically_exposed,"
                        + "preferred_channel,email_verified,phone_verified,num_complaints_last_year\n");
        for (int i = 0; i < count; i++) {
            String customerId = "CUST_%05d".formatted(startIndex + i);
            String firstName = pick(FIRST_NAMES);
            String lastName = pick(LAST_NAMES);
            String cityState = pick(CITIES_STATES);
            String[] parts = cityState.split(",");
            int age = 21 + random.nextInt(45);
            LocalDate dob = LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
            LocalDate since = LocalDate.now().minusDays(200 + random.nextInt(1200));
            String risk = weightedRisk();
            sb.append(customerId).append(',')
                    .append(firstName).append(',')
                    .append(lastName).append(',')
                    .append(random.nextBoolean() ? "M" : "F").append(',')
                    .append(dob).append(',')
                    .append(age).append(',')
                    .append(firstName.toLowerCase()).append('.').append(lastName.toLowerCase()).append(i)
                    .append("@example.com").append(',')
                    .append("+91-9").append(100000000 + random.nextInt(899999999)).append(',')
                    .append(parts[0]).append(',')
                    .append(parts[1]).append(',')
                    .append("IN,")
                    .append(100000 + random.nextInt(899999)).append(',')
                    .append(pick(OCCUPATIONS)).append(',')
                    .append(300000 + random.nextInt(2000000)).append(".0,")
                    .append("Married,Graduate,EMPLOYED,")
                    .append(since).append(',')
                    .append(random.nextInt(4) == 0 ? "PREMIUM" : "RETAIL").append(',')
                    .append("VERIFIED,")
                    .append(risk).append(',')
                    .append(risk.equals("HIGH") && random.nextInt(5) == 0 ? "1" : "0").append(',')
                    .append("Mobile Banking,Y,Y,")
                    .append(random.nextInt(3))
                    .append('\n');
        }
        return sb.toString();
    }

    public String generateAccountsCsv(List<String> customerIds, int startIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "account_id,customer_id,account_type,account_status,currency,open_date,close_date,branch_code,"
                        + "branch_city,current_balance,avg_monthly_balance_6m,credit_limit,credit_utilization_pct,"
                        + "overdraft_enabled,card_type,is_joint_account,num_linked_devices,mobile_banking_enrolled,"
                        + "last_login_date,avg_monthly_txn_count,account_tier\n");
        int accountIndex = startIndex;
        for (String customerId : customerIds) {
            int accountsForCustomer = 1 + (random.nextInt(4) == 0 ? 1 : 0);
            for (int a = 0; a < accountsForCustomer; a++) {
                String accountId = "ACC_%06d".formatted(accountIndex++);
                LocalDate openDate = LocalDate.now().minusDays(200 + random.nextInt(1200));
                sb.append(accountId).append(',')
                        .append(customerId).append(',')
                        .append(pick(new String[] {"SAVINGS", "CURRENT", "NRE"})).append(',')
                        .append("ACTIVE,INR,")
                        .append(openDate).append(",,")
                        .append("BR1").append(10 + random.nextInt(89)).append(',')
                        .append(pick(CITIES_STATES).split(",")[0]).append(',')
                        .append(10000 + random.nextInt(490000)).append(".0,")
                        .append(10000 + random.nextInt(300000)).append(".0,0.0,0.0,N,CLASSIC,0,")
                        .append(1 + random.nextInt(3)).append(",Y,")
                        .append(LocalDate.now().minusDays(random.nextInt(30))).append(',')
                        .append(5 + random.nextInt(30)).append(',')
                        .append(pick(new String[] {"BRONZE", "SILVER", "GOLD"}))
                        .append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Builds a transaction CSV that deliberately triggers every rule typology (using the first
     * few accounts passed in), tops up with 90 days of baseline history for the behavioral
     * deviation demo account, then fills the remainder with benign noise up to {@code totalTarget}
     * rows so the bulk-load performance target is exercised realistically.
     */
    public String generateTransactionsCsv(List<String> accountIds, int totalTarget) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "account_id,direction,amount,currency,counterparty_name,counterparty_account,"
                        + "counterparty_country,channel,jurisdiction,txn_timestamp\n");

        int rows = 0;
        Instant now = Instant.now();

        // 1) CTR threshold -- single large transaction.
        rows += writeRow(sb, accountIds.get(0), "DEBIT", "12500.00", "INR", "Property Deposit", null, "IN", "RTGS", null, now.minusSeconds(3600));

        // 2) Structuring -- 3 transactions just under the threshold within 24h.
        String structuringAccount = accountIds.get(1 % accountIds.size());
        rows += writeRow(sb, structuringAccount, "DEBIT", "9200.00", "INR", "Cash Withdrawal", null, "IN", "CARD", null, now.minusSeconds(3 * 3600));
        rows += writeRow(sb, structuringAccount, "DEBIT", "9500.00", "INR", "Cash Withdrawal", null, "IN", "CARD", null, now.minusSeconds(2 * 3600));
        rows += writeRow(sb, structuringAccount, "DEBIT", "9800.00", "INR", "Cash Withdrawal", null, "IN", "CARD", null, now.minusSeconds(3600));

        // 3) Rapid movement -- deposit then >=80% moved out within 48h.
        String rapidAccount = accountIds.get(2 % accountIds.size());
        rows += writeRow(sb, rapidAccount, "CREDIT", "50000.00", "INR", "Wire In", null, "IN", "WIRE", null, now.minusSeconds(20 * 3600));
        rows += writeRow(sb, rapidAccount, "DEBIT", "25000.00", "INR", "Transfer Out A", "EXT001", "IN", "IMPS", null, now.minusSeconds(10 * 3600));
        rows += writeRow(sb, rapidAccount, "DEBIT", "20000.00", "INR", "Transfer Out B", "EXT002", "IN", "IMPS", null, now.minusSeconds(2 * 3600));

        // 4) High-risk jurisdiction -- flagged regardless of amount.
        String hrjAccount = accountIds.get(3 % accountIds.size());
        rows += writeRow(sb, hrjAccount, "DEBIT", "4200.00", "INR", "Overseas Trading Co", "IR-9981", "IR", "WIRE", "IR", now.minusSeconds(4 * 3600));

        // 5) Round-number pattern -- repeated exact multiples of 1000 in a 24h window.
        String roundAccount = accountIds.get(4 % accountIds.size());
        rows += writeRow(sb, roundAccount, "DEBIT", "5000.00", "INR", "Cash Withdrawal", null, "IN", "CARD", null, now.minusSeconds(6 * 3600));
        rows += writeRow(sb, roundAccount, "DEBIT", "10000.00", "INR", "Cash Withdrawal", null, "IN", "CARD", null, now.minusSeconds(4 * 3600));
        rows += writeRow(sb, roundAccount, "DEBIT", "15000.00", "INR", "Cash Withdrawal", null, "IN", "CARD", null, now.minusSeconds(2 * 3600));

        // 6) Behavioral deviation -- ~90 days of a modest baseline, then a spike today.
        String deviationAccount = accountIds.get(5 % accountIds.size());
        for (int day = 90; day >= 1; day--) {
            BigDecimalLike amount = BigDecimalLike.of(800 + random.nextInt(400));
            rows +=
                    writeRow(
                            sb,
                            deviationAccount,
                            "DEBIT",
                            amount.value(),
                            "INR",
                            pick(BENIGN_COUNTERPARTIES),
                            null,
                            "IN",
                            pick(CHANNELS),
                            null,
                            now.minusSeconds(day * 86400L));
        }
        rows +=
                writeRow(
                        sb, deviationAccount, "DEBIT", "18000.00", "INR", "Unusual Large Purchase", null, "IN", "CARD",
                        null, now.minusSeconds(600));

        // 7) Benign noise across all accounts to reach the bulk-load target size.
        while (rows < totalTarget) {
            String account = accountIds.get(random.nextInt(accountIds.size()));
            String direction = random.nextBoolean() ? "DEBIT" : "CREDIT";
            BigDecimalLike amount = BigDecimalLike.of(200 + random.nextInt(4000));
            int daysAgo = random.nextInt(60);
            rows +=
                    writeRow(
                            sb,
                            account,
                            direction,
                            amount.value(),
                            "INR",
                            pick(BENIGN_COUNTERPARTIES),
                            null,
                            "IN",
                            pick(CHANNELS),
                            null,
                            now.minusSeconds(daysAgo * 86400L + random.nextInt(86400)));
        }

        return sb.toString();
    }

    private int writeRow(
            StringBuilder sb,
            String accountId,
            String direction,
            String amount,
            String currency,
            String counterpartyName,
            String counterpartyAccount,
            String counterpartyCountry,
            String channel,
            String jurisdiction,
            Instant timestamp) {
        sb.append(accountId).append(',')
                .append(direction).append(',')
                .append(amount).append(',')
                .append(currency).append(',')
                .append(counterpartyName == null ? "" : counterpartyName).append(',')
                .append(counterpartyAccount == null ? "" : counterpartyAccount).append(',')
                .append(counterpartyCountry == null ? "" : counterpartyCountry).append(',')
                .append(channel).append(',')
                .append(jurisdiction == null ? "" : jurisdiction).append(',')
                .append(timestamp.atOffset(ZoneOffset.UTC))
                .append('\n');
        return 1;
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }

    private String weightedRisk() {
        int roll = random.nextInt(10);
        if (roll < 6) {
            return "LOW";
        } else if (roll < 9) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    /** Tiny helper so amount formatting stays consistent without importing BigDecimal everywhere above. */
    private record BigDecimalLike(java.math.BigDecimal decimal) {
        static BigDecimalLike of(int whole) {
            return new BigDecimalLike(new java.math.BigDecimal(whole).setScale(2));
        }

        String value() {
            return decimal.toPlainString();
        }
    }
}
