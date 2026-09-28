package com.mockingbirdbank.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void masksAllButLastFourDigits() {
        Account account =
                new Account(
                        null,
                        "1000000004821",
                        AccountType.CHECKING,
                        "Everyday Checking",
                        BigDecimal.TEN,
                        LocalDate.now());

        assertThat(account.getMaskedNumber()).isEqualTo("•••• 4821");
    }

    @Test
    void masksShortOrMissingNumbersSafely() {
        Account shortNumber =
                new Account(
                        null, "12", AccountType.CHECKING, null, BigDecimal.ZERO, LocalDate.now());
        Account noNumber =
                new Account(
                        null, null, AccountType.CHECKING, null, BigDecimal.ZERO, LocalDate.now());

        assertThat(shortNumber.getMaskedNumber()).isEqualTo("••••");
        assertThat(noNumber.getMaskedNumber()).isEqualTo("••••");
    }

    @Test
    void displayNameFallsBackToAccountTypeWhenNicknameBlank() {
        Account blankNickname =
                new Account(
                        null,
                        "1000000004821",
                        AccountType.SAVINGS,
                        "   ",
                        BigDecimal.ZERO,
                        LocalDate.now());
        Account noNickname =
                new Account(
                        null,
                        "1000000004821",
                        AccountType.SAVINGS,
                        null,
                        BigDecimal.ZERO,
                        LocalDate.now());

        assertThat(blankNickname.getDisplayName()).isEqualTo("SAVINGS");
        assertThat(noNickname.getDisplayName()).isEqualTo("SAVINGS");
    }

    @Test
    void displayNameUsesNicknameWhenPresent() {
        Account withNickname =
                new Account(
                        null,
                        "1000000004821",
                        AccountType.SAVINGS,
                        "Vacation Savings",
                        BigDecimal.ZERO,
                        LocalDate.now());

        assertThat(withNickname.getDisplayName()).isEqualTo("Vacation Savings");
    }
}
