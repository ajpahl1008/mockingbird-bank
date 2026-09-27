package com.mockingbirdbank.service;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.model.AppUser;
import com.mockingbirdbank.repository.AccountRepository;
import com.mockingbirdbank.repository.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AppUserRepository appUserRepository;
    private final AccountRepository accountRepository;

    public AccountService(AppUserRepository appUserRepository, AccountRepository accountRepository) {
        this.appUserRepository = appUserRepository;
        this.accountRepository = accountRepository;
    }

    /** Resolves the holder linked to whoever Spring Security says is signed in. */
    public AccountHolder currentHolder() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .map(AppUser::getHolder)
                .orElseThrow(() -> new IllegalStateException("No account holder linked to user: " + username));
    }

    public List<Account> accountsFor(AccountHolder holder) {
        return accountRepository.findByHolderIdOrderByAccountTypeAscIdAsc(holder.getId());
    }

    public Account requireAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("No such account: " + accountId));
    }

    public BigDecimal totalBalance(AccountHolder holder) {
        return accountsFor(holder).stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
