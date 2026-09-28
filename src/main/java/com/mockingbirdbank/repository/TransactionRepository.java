package com.mockingbirdbank.repository;

import com.mockingbirdbank.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByPostedAtDesc(Long accountId);
}
