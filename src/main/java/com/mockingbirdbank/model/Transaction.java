package com.mockingbirdbank.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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

    public Transaction(Account account, OffsetDateTime postedAt, String description,
                        String category, BigDecimal amount) {
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
