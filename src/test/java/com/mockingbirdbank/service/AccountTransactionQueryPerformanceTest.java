package com.mockingbirdbank.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.model.AccountType;
import com.mockingbirdbank.model.Transaction;
import com.mockingbirdbank.repository.AccountHolderRepository;
import com.mockingbirdbank.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Guards against N+1 queries when several accounts' transaction histories are loaded together.
 *
 * <p>Uses Hibernate's {@link Statistics} API to count the actual SQL statements executed rather
 * than eyeballing logs. If someone removes the {@code @BatchSize} on {@code Account.transactions}
 * (see that field) or otherwise reintroduces a per-account query loop, the statement count grows
 * linearly with the account count and this test fails - catching the regression here instead of as
 * a prod latency surprise.
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class AccountTransactionQueryPerformanceTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final int ACCOUNT_COUNT = 6;
    private static final int TRANSACTIONS_PER_ACCOUNT = 2;

    @Autowired private AccountHolderRepository holderRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private EntityManager entityManager;

    @Test
    @Transactional
    void loadingSeveralAccountsTransactionsDoesNotIssueOneQueryPerAccount() {
        AccountHolder holder = seedHolderWithAccountsAndTransactions();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        // Force the next reads to hit the database instead of the already-populated
        // session cache, so the statement count reflects real query behavior.
        entityManager.clear();
        statistics.clear();

        List<Account> accounts =
                accountRepository.findByHolderIdOrderByAccountTypeAscIdAsc(holder.getId());
        long transactionTotal =
                accounts.stream().mapToLong(account -> account.getTransactions().size()).sum();

        assertThat(transactionTotal).isEqualTo((long) ACCOUNT_COUNT * TRANSACTIONS_PER_ACCOUNT);
        // 1 query for the accounts, plus a small, batched number of transaction-collection
        // fetches (not one SELECT per account, which is what an N+1 would look like).
        assertThat(statistics.getPrepareStatementCount())
                .as(
                        "expected the accounts query plus a small batched number of "
                                + "transaction-collection fetches, not one query per account "
                                + "(N+1); saw %d accounts loaded with %d SQL statements",
                        ACCOUNT_COUNT, statistics.getPrepareStatementCount())
                .isLessThan(ACCOUNT_COUNT);
    }

    private AccountHolder seedHolderWithAccountsAndTransactions() {
        AccountHolder holder =
                holderRepository.save(
                        new AccountHolder("N+1 Guard Holder", "n-plus-one-guard@example.com"));
        for (int i = 0; i < ACCOUNT_COUNT; i++) {
            Account account =
                    accountRepository.save(
                            new Account(
                                    holder,
                                    "N1-ACCT-" + i,
                                    AccountType.CHECKING,
                                    "Test account " + i,
                                    BigDecimal.TEN,
                                    LocalDate.now()));
            for (int t = 0; t < TRANSACTIONS_PER_ACCOUNT; t++) {
                account.getTransactions().add(transaction(account));
            }
        }
        entityManager.flush();
        return holder;
    }

    private Transaction transaction(Account account) {
        return new Transaction(
                account, OffsetDateTime.now(), "Test transaction", "General", BigDecimal.ONE);
    }
}
