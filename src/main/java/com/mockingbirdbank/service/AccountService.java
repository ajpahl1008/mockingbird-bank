package com.mockingbirdbank.service;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.repository.AccountHolderRepository;
import com.mockingbirdbank.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountHolderRepository holderRepository;
    private final AccountRepository accountRepository;

    public AccountService(AccountHolderRepository holderRepository, AccountRepository accountRepository) {
        this.holderRepository = holderRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * v1 is single-tenant: there's exactly one signed-in-as holder.
     * Swap this for a real authentication lookup once login exists.
     */
    public AccountHolder currentHolder() {
        return holderRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No account holder seeded"));
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
