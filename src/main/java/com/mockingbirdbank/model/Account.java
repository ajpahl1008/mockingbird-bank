package com.mockingbirdbank.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "holder_id")
    private AccountHolder holder;

    /** Full account number, stored as-is; only ever masked in the UI. */
    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    /** Display name, e.g. "Vacation Savings". */
    private String nickname;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate openedAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    public Account(AccountHolder holder, String accountNumber, AccountType accountType,
                    String nickname, BigDecimal balance, LocalDate openedAt) {
        this.holder = holder;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.nickname = nickname;
        this.balance = balance;
        this.openedAt = openedAt;
    }

    /** Masked for display: "•••• 4821". */
    public String getMaskedNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "••••";
        }
        return "•••• " + accountNumber.substring(accountNumber.length() - 4);
    }

    public String getDisplayName() {
        return nickname != null && !nickname.isBlank() ? nickname : accountType.name();
    }
}
