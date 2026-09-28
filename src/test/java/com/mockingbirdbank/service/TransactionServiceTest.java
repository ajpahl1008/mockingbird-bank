package com.mockingbirdbank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mockingbirdbank.model.Transaction;
import com.mockingbirdbank.repository.TransactionRepository;
import com.mockingbirdbank.resilience.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @Test
    void forAccountDelegatesToRepositoryOrderedByMostRecent() {
        TransactionService service =
                new TransactionService(
                        transactionRepository,
                        CircuitBreakerRegistry.of(ResilienceConfig.defaultConfig()));
        Transaction tx =
                new Transaction(
                        null, OffsetDateTime.now(), "Coffee", "Dining", new BigDecimal("-4.50"));
        when(transactionRepository.findByAccountIdOrderByPostedAtDesc(42L)).thenReturn(List.of(tx));

        assertThat(service.forAccount(42L)).containsExactly(tx);
    }
}
