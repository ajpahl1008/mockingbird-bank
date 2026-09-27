package com.mockingbirdbank.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    void positiveAmountIsCredit() {
        Transaction credit = new Transaction(null, OffsetDateTime.now(), "Deposit",
                "Income", new BigDecimal("100.00"));

        assertThat(credit.isCredit()).isTrue();
    }

    @Test
    void negativeAmountIsNotCredit() {
        Transaction debit = new Transaction(null, OffsetDateTime.now(), "Coffee",
                "Dining", new BigDecimal("-4.50"));

        assertThat(debit.isCredit()).isFalse();
    }

    @Test
    void missingAmountIsNotCredit() {
        Transaction blank = new Transaction();

        assertThat(blank.isCredit()).isFalse();
    }
}
