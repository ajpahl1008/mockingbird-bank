package com.mockingbirdbank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false)
    private OffsetDateTime postedAt;

    @Column(nullable = false)
    private String description;

    private String category;

    /** Signed amount: negative = debit, positive = credit. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    public Transaction(
            Account account,
            OffsetDateTime postedAt,
            String description,
            String category,
            BigDecimal amount) {
        this.account = account;
        this.postedAt = postedAt;
        this.description = description;
        this.category = category;
        this.amount = amount;
    }

    public boolean isCredit() {
        return amount != null && amount.signum() > 0;
    }
}
