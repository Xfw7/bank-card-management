package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.security.SecurityUtils;
import com.example.bankcards.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferMapper transferMapper;

    @InjectMocks
    private TransferService transferService;

    private final User owner = TestFixtures.user(10L, "john");

    @Test
    void transfer_updatesBalancesAndPersistsTransfer() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("500.00"));
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("100.00"));
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("150.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer saved = invocation.getArgument(0);
            saved.setId(99L);
            saved.setCreatedAt(Instant.parse("2026-07-09T12:00:00Z"));
            return saved;
        });
        when(transferMapper.toResponse(any(Transfer.class))).thenReturn(
                new TransferResponse(99L, 1L, 2L, new BigDecimal("150.00"),
                        Instant.parse("2026-07-09T12:00:00Z")));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            TransferResponse response = transferService.transfer(request);

            assertThat(fromCard.getBalance()).isEqualByComparingTo("350.00");
            assertThat(toCard.getBalance()).isEqualByComparingTo("250.00");
            assertThat(response.amount()).isEqualByComparingTo("150.00");

            ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);
            verify(transferRepository).save(captor.capture());
            assertThat(captor.getValue().getFromCard()).isSameAs(fromCard);
            assertThat(captor.getValue().getToCard()).isSameAs(toCard);
        }
    }

    @Test
    void transfer_locksCardsInAscendingIdOrder() {
        Card card1 = TestFixtures.activeCard(1L, owner, new BigDecimal("200.00"));
        Card card2 = TestFixtures.activeCard(2L, owner, new BigDecimal("200.00"));
        TransferRequest request = new TransferRequest(2L, 1L, new BigDecimal("50.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(card1));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(card2));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferMapper.toResponse(any(Transfer.class))).thenReturn(
                new TransferResponse(1L, 2L, 1L, new BigDecimal("50.00"), Instant.now()));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            transferService.transfer(request);

            InOrder inOrder = inOrder(cardRepository);
            inOrder.verify(cardRepository).findByIdForUpdate(1L);
            inOrder.verify(cardRepository).findByIdForUpdate(2L);
        }
    }

    @Test
    void transfer_sameCard_throwsSameCardTransfer() {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("10.00"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SAME_CARD_TRANSFER);
        }
    }

    @Test
    void transfer_insufficientBalance_throwsInsufficientBalance() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("10.00"));
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("0.00"));
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("50.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_cardNotFound_throwsNotFound() {
        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("1.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_foreignCard_throwsForbidden() {
        User other = TestFixtures.user(20L, "other");
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));
        Card toCard = TestFixtures.activeCard(2L, other, new BigDecimal("0.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_blockedCard_throwsCardBlocked() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));
        fromCard.setStatus(CardStatus.BLOCKED);
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("0.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CARD_BLOCKED);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_expiredCard_throwsCardExpired() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));
        fromCard.setExpiryDate(LocalDate.now().minusDays(1));
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("0.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CARD_EXPIRED);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_secondCardNotFound_throwsNotFound() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_foreignFromCard_throwsForbidden() {
        User other = TestFixtures.user(20L, "other");
        Card fromCard = TestFixtures.activeCard(1L, other, new BigDecimal("100.00"));
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("0.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_toCardBlocked_throwsCardBlocked() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("0.00"));
        toCard.setStatus(CardStatus.BLOCKED);

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CARD_BLOCKED);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_toCardExpired_throwsCardExpired() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("0.00"));
        toCard.setExpiryDate(LocalDate.now().minusDays(1));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CARD_EXPIRED);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void transfer_expiredStatus_throwsCardExpired() {
        Card fromCard = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));
        fromCard.setStatus(CardStatus.EXPIRED);
        Card toCard = TestFixtures.activeCard(2L, owner, new BigDecimal("0.00"));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CARD_EXPIRED);

            verify(transferRepository, never()).save(any());
        }
    }

    @Test
    void getMyTransfers_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Card fromCard = TestFixtures.activeCard(1L, owner, BigDecimal.ZERO);
        Card toCard = TestFixtures.activeCard(2L, owner, BigDecimal.ZERO);
        Transfer transfer = Transfer.builder()
                .id(5L)
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(new BigDecimal("25.00"))
                .createdAt(Instant.parse("2026-07-09T12:00:00Z"))
                .build();
        TransferResponse transferResponse = new TransferResponse(
                5L, 1L, 2L, new BigDecimal("25.00"), transfer.getCreatedAt());
        Page<Transfer> transferPage = new PageImpl<>(List.of(transfer), pageable, 1);

        when(transferRepository.findByUserId(eq(10L), eq(pageable))).thenReturn(transferPage);
        when(transferMapper.toResponse(transfer)).thenReturn(transferResponse);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            PageResponse<TransferResponse> result = transferService.getMyTransfers(pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().amount()).isEqualByComparingTo("25.00");
            assertThat(result.totalElements()).isEqualTo(1);
            verify(transferRepository).findByUserId(10L, pageable);
        }
    }

    @Test
    void getMyCardTransfers_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Card fromCard = TestFixtures.activeCard(1L, owner, BigDecimal.ZERO);
        Card toCard = TestFixtures.activeCard(2L, owner, BigDecimal.ZERO);
        Transfer transfer = Transfer.builder()
                .id(7L)
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(new BigDecimal("40.00"))
                .createdAt(Instant.parse("2026-07-09T13:00:00Z"))
                .build();
        TransferResponse transferResponse = new TransferResponse(
                7L, 1L, 2L, new BigDecimal("40.00"), transfer.getCreatedAt());
        Page<Transfer> transferPage = new PageImpl<>(List.of(transfer), pageable, 1);

        when(transferRepository.findByCardIdAndUserId(eq(1L), eq(10L), eq(pageable))).thenReturn(transferPage);
        when(transferMapper.toResponse(transfer)).thenReturn(transferResponse);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            PageResponse<TransferResponse> result = transferService.getMyCardTransfers(1L, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().fromCardId()).isEqualTo(1L);
            verify(transferRepository).findByCardIdAndUserId(1L, 10L, pageable);
        }
    }
}
