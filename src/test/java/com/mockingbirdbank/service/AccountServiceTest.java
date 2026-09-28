package com.mockingbirdbank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.model.AccountType;
import com.mockingbirdbank.model.AppUser;
import com.mockingbirdbank.repository.AccountRepository;
import com.mockingbirdbank.repository.AppUserRepository;
import com.mockingbirdbank.resilience.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService =
                new AccountService(
                        appUserRepository,
                        accountRepository,
                        CircuitBreakerRegistry.of(ResilienceConfig.defaultConfig()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(username, "n/a", List.of()));
    }

    @Test
    void currentHolderResolvesHolderLinkedToSignedInUser() {
        AccountHolder holder = new AccountHolder("Jordan Ellis", "jordan.ellis@example.com");
        AppUser appUser = new AppUser("jordan.ellis", "hashed", holder);
        authenticateAs("jordan.ellis");
        when(appUserRepository.findByUsername("jordan.ellis")).thenReturn(Optional.of(appUser));

        assertThat(accountService.currentHolder()).isSameAs(holder);
    }

    @Test
    void currentHolderFailsFastWhenSignedInUserHasNoLinkedAccount() {
        authenticateAs("ghost");
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(accountService::currentHolder).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void totalBalanceSumsAllAccountsForHolder() {
        AccountHolder holder = new AccountHolder("Jordan Ellis", "jordan.ellis@example.com");
        holder.setId(1L);
        Account checking =
                new Account(
                        holder,
                        "1",
                        AccountType.CHECKING,
                        "Checking",
                        new BigDecimal("100.00"),
                        LocalDate.now());
        Account savings =
                new Account(
                        holder,
                        "2",
                        AccountType.SAVINGS,
                        "Savings",
                        new BigDecimal("250.50"),
                        LocalDate.now());
        when(accountRepository.findByHolderIdOrderByAccountTypeAscIdAsc(1L))
                .thenReturn(List.of(checking, savings));

        assertThat(accountService.totalBalance(holder)).isEqualByComparingTo("350.50");
    }

    @Test
    void requireAccountThrowsForUnknownId() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.requireAccount(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireAccountReturnsMatchFromRepository() {
        Account account =
                new Account(
                        new AccountHolder("Jordan Ellis", "jordan.ellis@example.com"),
                        "1",
                        AccountType.CHECKING,
                        "Checking",
                        BigDecimal.ZERO,
                        LocalDate.now());
        when(accountRepository.findById(7L)).thenReturn(Optional.of(account));

        assertThat(accountService.requireAccount(7L)).isSameAs(account);
    }
}
