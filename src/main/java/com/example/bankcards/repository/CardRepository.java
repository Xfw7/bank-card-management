package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<Card> findByIdAndDeletedAtIsNull(Long id);

    // Serializes concurrent transfers on the same card (SELECT FOR UPDATE).
    // JOIN FETCH user avoids a secondary SELECT when assertOwnership reads card.getUser().
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c JOIN FETCH c.user WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Card> findByIdForUpdate(@Param("id") Long id);

    boolean existsByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT c FROM Card c
            WHERE c.user.id = :userId
              AND c.deletedAt IS NULL
              AND (:status IS NULL OR c.status = :status)
              AND (:lastFour IS NULL OR c.lastFour = :lastFour)
            """)
    Page<Card> findUserActiveCards(@Param("userId") Long userId,
                                   @Param("status") CardStatus status,
                                   @Param("lastFour") String lastFour,
                                   Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT c FROM Card c
            WHERE c.deletedAt IS NULL
              AND (:status IS NULL OR c.status = :status)
              AND (:lastFour IS NULL OR c.lastFour = :lastFour)
              AND (:userId IS NULL OR c.user.id = :userId)
            """)
    Page<Card> findAllActiveCards(@Param("userId") Long userId,
                                  @Param("status") CardStatus status,
                                  @Param("lastFour") String lastFour,
                                  Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Card> findByBlockRequestedTrueAndDeletedAtIsNull(Pageable pageable);

    @Query("""
            SELECT c FROM Card c
            WHERE c.status = com.example.bankcards.entity.enums.CardStatus.ACTIVE
              AND c.expiryDate < :today
              AND c.deletedAt IS NULL
            """)
    List<Card> findActiveExpiredCards(@Param("today") LocalDate today);
}
