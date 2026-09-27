package com.mockingbirdbank.config;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.model.AccountType;
import com.mockingbirdbank.model.AppUser;
import com.mockingbirdbank.model.Transaction;
import com.mockingbirdbank.repository.AccountHolderRepository;
import com.mockingbirdbank.repository.AccountRepository;
import com.mockingbirdbank.repository.AppUserRepository;
import com.mockingbirdbank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Seeds one account holder (plus a matching login) with a few accounts and
 * sample transactions so the dashboard has something real to show. Runs only
 * on an empty database - safe to leave on in dev; swap for a proper
 * migration tool before this becomes a shared environment.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // Public (not just package-visible) so the smoke test can assert
    // against the seeded login directly instead of duplicating the values -
    // keeps the seed and its test coverage from silently drifting apart.
    public static final String DEV_USERNAME = "jordan.ellis";
    public static final String DEV_PASSWORD_ENV = "MOCKINGBIRD_DEV_PASSWORD";
    public static final String DEV_PASSWORD_DEFAULT = "mockingbird";

    private final AccountHolderRepository holderRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AccountHolderRepository holderRepository,
                            AccountRepository accountRepository,
                            TransactionRepository transactionRepository,
                            AppUserRepository appUserRepository,
                            PasswordEncoder passwordEncoder) {
        this.holderRepository = holderRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (holderRepository.count() > 0) {
            return;
        }

        AccountHolder holder = holderRepository.save(
                new AccountHolder("Jordan Ellis", "jordan.ellis@example.com"));

        String devPassword = System.getenv().getOrDefault(DEV_PASSWORD_ENV, DEV_PASSWORD_DEFAULT);
        appUserRepository.save(new AppUser(DEV_USERNAME, passwordEncoder.encode(devPassword), holder));
        log.info("Seeded DEMO login for local/dev use only - username: {}, password: {} (override via {})",
                DEV_USERNAME, devPassword, DEV_PASSWORD_ENV);

        Account checking = accountRepository.save(new Account(
                holder, "1000000004821", AccountType.CHECKING, "Everyday Checking",
                new BigDecimal("4318.52"), LocalDate.of(2021, 3, 12)));

        Account savings = accountRepository.save(new Account(
                holder, "2000000007743", AccountType.SAVINGS, "High-Yield Savings",
                new BigDecimal("18200.00"), LocalDate.of(2022, 1, 5)));

        Account vacation = accountRepository.save(new Account(
                holder, "2000000000092", AccountType.SAVINGS, "Vacation Savings",
                new BigDecimal("1800.00"), LocalDate.of(2023, 6, 20)));

        seedTransactions(checking);
        seedTransactions(savings);
        seedTransactions(vacation);
    }

    private void seedTransactions(Account account) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Object[][] rows = {
                {0, "Trader Joe's", "Groceries", "-86.42"},
                {1, "Direct Deposit - Bespin Engineering", "Income", "3200.00"},
                {2, "Green Valley Utilities", "Utilities", "-142.10"},
                {4, "Transfer to Savings", "Transfer", "-500.00"},
                {6, "Blue Bottle Coffee", "Dining", "-6.75"},
                {9, "Rent - Meridian Apartments", "Housing", "-1450.00"},
                {12, "Amazon", "Shopping", "-58.19"},
                {14, "Direct Deposit - Bespin Engineering", "Income", "3200.00"}
        };
        for (Object[] row : rows) {
            int daysAgo = (int) row[0];
            transactionRepository.save(new Transaction(
                    account,
                    now.minusDays(daysAgo),
                    (String) row[1],
                    (String) row[2],
                    new BigDecimal((String) row[3])));
        }
    }
}
