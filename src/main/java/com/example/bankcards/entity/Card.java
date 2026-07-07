package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "encrypted_pan", nullable = false, length = 512)
    private String encryptedPan;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "block_requested", nullable = false)
    @Builder.Default
    private boolean blockRequested = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isExpired(LocalDate today) {
        return expiryDate.isBefore(today);
    }

    public boolean isActive() {
        return status == CardStatus.ACTIVE && !isDeleted();
    }

    public boolean canBeUsedForTransfer(LocalDate today) {
        return isActive() && !isExpired(today);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public void requestBlock() {
        assertNotDeleted();
        if (status == CardStatus.BLOCKED) {
            throw new IllegalStateException("Card is already blocked");
        }
        blockRequested = true;
    }

    public void block() {
        assertNotDeleted();
        status = CardStatus.BLOCKED;
        blockRequested = false;
    }

    public void activate() {
        assertNotDeleted();
        status = CardStatus.ACTIVE;
        blockRequested = false;
    }

    public void markExpired() {
        assertNotDeleted();
        status = CardStatus.EXPIRED;
    }

    public void markDeleted() {
        if (isDeleted()) {
            return;
        }
        deletedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        assertPositiveAmount(amount);
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        assertPositiveAmount(amount);
        balance = balance.add(amount);
    }

    private void assertNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("Card is deleted");
        }
    }

    private static void assertPositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
