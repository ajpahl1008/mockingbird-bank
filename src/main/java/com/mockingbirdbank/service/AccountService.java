package com.mockingbirdbank.service;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.model.AppUser;
import com.mockingbirdbank.repository.AccountRepository;
import com.mockingbirdbank.repository.AppUserRepository;
import com.mockingbirdbank.resilience.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AppUserRepository appUserRepository;
    private final AccountRepository accountRepository;
    private final CircuitBreaker circuitBreaker;

    public AccountService(
            AppUserRepository appUserRepository,
            AccountRepository accountRepository,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.appUserRepository = appUserRepository;
        this.accountRepository = accountRepository;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("accounts");
    }

    /** Resolves the holder linked to whoever Spring Security says is signed in. */
    public AccountHolder currentHolder() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<AppUser> appUser = protectedCall(() -> appUserRepository.findByUsername(username));
        return appUser.map(AppUser::getHolder)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No account holder linked to user: " + username));
    }

    public List<Account> accountsFor(AccountHolder holder) {
        return protectedCall(
                () -> accountRepository.findByHolderIdOrderByAccountTypeAscIdAsc(holder.getId()));
    }

    public Account requireAccount(Long accountId) {
        Optional<Account> account = protectedCall(() -> accountRepository.findById(accountId));
        return account.orElseThrow(
                () -> new IllegalArgumentException("No such account: " + accountId));
    }

    /**
     * Only wraps the actual repository call, never the {@code orElseThrow} above it - a business
     * outcome like "no such account" must not count as a circuit-breaker failure the way a real
     * database error does.
     */
    private <T> T protectedCall(Supplier<T> repositoryCall) {
        try {
            return circuitBreaker.executeSupplier(repositoryCall);
        } catch (CallNotPermittedException e) {
            throw new ServiceUnavailableException("Accounts are temporarily unavailable", e);
        }
    }

    public BigDecimal totalBalance(AccountHolder holder) {
        return accountsFor(holder).stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
