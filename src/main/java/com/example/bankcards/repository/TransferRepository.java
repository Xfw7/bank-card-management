package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @EntityGraph(attributePaths = {"fromCard", "toCard"})
    @Query("""
            SELECT t FROM Transfer t
            WHERE t.fromCard.user.id = :userId OR t.toCard.user.id = :userId
            """)
    Page<Transfer> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"fromCard", "toCard"})
    @Query("""
            SELECT t FROM Transfer t
            WHERE (t.fromCard.id = :cardId OR t.toCard.id = :cardId)
              AND EXISTS (
                  SELECT c FROM Card c
                  WHERE c.id = :cardId AND c.user.id = :userId AND c.deletedAt IS NULL
              )
            """)
    Page<Transfer> findByCardIdAndUserId(@Param("cardId") Long cardId,
                                         @Param("userId") Long userId,
                                         Pageable pageable);
}
