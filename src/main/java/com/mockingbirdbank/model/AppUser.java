package com.mockingbirdbank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Login credentials, kept in their own table and deliberately separate from
 * {@link AccountHolder} (banking/PII data) so the two concerns don't mix.
 * One row per holder - this app has exactly one user today, not a
 * registration system.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt hash - the raw password is never stored or logged. */
    @Column(nullable = false)
    private String passwordHash;

    @OneToOne(optional = false)
    @JoinColumn(name = "holder_id", nullable = false, unique = true)
    private AccountHolder holder;

    public AppUser(String username, String passwordHash, AccountHolder holder) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.holder = holder;
    }
}
