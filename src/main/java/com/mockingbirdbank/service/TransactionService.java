package com.mockingbirdbank.service;

import com.mockingbirdbank.model.Transaction;
import com.mockingbirdbank.repository.TransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> forAccount(Long accountId) {
        return transactionRepository.findByAccountIdOrderByPostedAtDesc(accountId);
    }
}
