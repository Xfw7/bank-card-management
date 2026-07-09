package com.example.bankcards.support;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.security.UserPrincipal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .passwordHash("hash")
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    public static UserPrincipal userPrincipal(Long id, String username) {
        return new UserPrincipal(id, username, "hash", Role.USER, true,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    public static Card activeCard(Long id, User owner, BigDecimal balance) {
        return Card.builder()
                .id(id)
                .user(owner)
                .encryptedPan("encrypted")
                .lastFour("1234")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(balance)
                .blockRequested(false)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
