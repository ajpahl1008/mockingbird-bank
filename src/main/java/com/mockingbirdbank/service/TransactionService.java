package com.mockingbirdbank.service;

import com.mockingbirdbank.model.Transaction;
import com.mockingbirdbank.repository.TransactionRepository;
import com.mockingbirdbank.resilience.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CircuitBreaker circuitBreaker;

    public TransactionService(
            TransactionRepository transactionRepository,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.transactionRepository = transactionRepository;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("transactions");
    }

    public List<Transaction> forAccount(Long accountId) {
        return protectedCall(
                () -> transactionRepository.findByAccountIdOrderByPostedAtDesc(accountId));
    }

    private <T> T protectedCall(Supplier<T> repositoryCall) {
        try {
            return circuitBreaker.executeSupplier(repositoryCall);
        } catch (CallNotPermittedException e) {
            throw new ServiceUnavailableException(
                    "Transaction history is temporarily unavailable", e);
        }
    }
}
