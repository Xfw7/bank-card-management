package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.SecurityUtils;
import com.example.bankcards.support.TestFixtures;
import com.example.bankcards.util.AesEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private AesEncryptor aesEncryptor;

    @InjectMocks
    private CardService cardService;

    private final User owner = TestFixtures.user(10L, "john");

    @Test
    void create_encryptsPanAndSavesCard() {
        CreateCardRequest request = new CreateCardRequest(
                "4111111111111111",
                LocalDate.now().plusYears(2),
                new BigDecimal("1000.00"),
                "john"
        );
        CardResponse response = new CardResponse(
                1L, "**** **** **** 1111", "john",
                request.expiryDate(), CardStatus.ACTIVE, request.balance(),
                false, Instant.parse("2026-07-09T12:00:00Z"));

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(owner));
        when(aesEncryptor.encrypt("4111111111111111")).thenReturn("cipher-text");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(1L);
            return card;
        });
        when(cardMapper.toResponse(any(Card.class))).thenReturn(response);

        CardResponse result = cardService.create(request);

        assertThat(result.maskedNumber()).isEqualTo("**** **** **** 1111");

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        Card saved = captor.getValue();
        assertThat(saved.getEncryptedPan()).isEqualTo("cipher-text");
        assertThat(saved.getLastFour()).isEqualTo("1111");
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void create_unknownOwner_throwsNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        CreateCardRequest request = new CreateCardRequest(
                "4111111111111111",
                LocalDate.now().plusYears(1),
                BigDecimal.ZERO,
                "missing"
        );

        assertThatThrownBy(() -> cardService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void requestBlock_marksCardAsRequested() {
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("10.00"));
        CardResponse response = new CardResponse(
                1L, "**** **** **** 1234", "john",
                card.getExpiryDate(), CardStatus.ACTIVE, card.getBalance(),
                true, Instant.now());

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(response);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            CardResponse result = cardService.requestBlock(1L);

            assertThat(card.isBlockRequested()).isTrue();
            assertThat(result.blockRequested()).isTrue();
        }
    }

    @Test
    void requestBlock_alreadyBlocked_throwsCardBlocked() {
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("10.00"));
        card.setStatus(CardStatus.BLOCKED);

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> cardService.requestBlock(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CARD_BLOCKED);
        }
    }

    @Test
    void requestBlock_notOwner_throwsForbidden() {
        User other = TestFixtures.user(20L, "other");
        Card card = TestFixtures.activeCard(1L, other, new BigDecimal("10.00"));

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> cardService.requestBlock(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Test
    void markExpiredCards_updatesExpiredActiveCards() {
        Card expired = TestFixtures.activeCard(1L, owner, BigDecimal.ZERO);
        expired.setExpiryDate(LocalDate.now().minusDays(1));

        when(cardRepository.findActiveExpiredCards(any(LocalDate.class))).thenReturn(List.of(expired));

        int count = cardService.markExpiredCards();

        assertThat(count).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(CardStatus.EXPIRED);
    }

    @Test
    void getAll_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("100.00"));
        CardResponse cardResponse = new CardResponse(
                1L, "**** **** **** 1234", "john",
                card.getExpiryDate(), CardStatus.ACTIVE, card.getBalance(),
                false, Instant.parse("2026-07-09T12:00:00Z"));
        Page<Card> cardPage = new PageImpl<>(List.of(card), pageable, 1);

        when(cardRepository.findAllActiveCards(10L, CardStatus.ACTIVE, "1234", pageable))
                .thenReturn(cardPage);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        PageResponse<CardResponse> result = cardService.getAll(10L, CardStatus.ACTIVE, "1234", pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().maskedNumber()).isEqualTo("**** **** **** 1234");
        assertThat(result.totalElements()).isEqualTo(1);
        verify(cardRepository).findAllActiveCards(10L, CardStatus.ACTIVE, "1234", pageable);
    }

    @Test
    void getPendingBlockRequests_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Card card = TestFixtures.activeCard(2L, owner, BigDecimal.ZERO);
        card.setBlockRequested(true);
        CardResponse cardResponse = new CardResponse(
                2L, "**** **** **** 1234", "john",
                card.getExpiryDate(), CardStatus.ACTIVE, card.getBalance(),
                true, Instant.parse("2026-07-09T12:00:00Z"));
        Page<Card> cardPage = new PageImpl<>(List.of(card), pageable, 1);

        when(cardRepository.findByBlockRequestedTrueAndDeletedAtIsNull(pageable)).thenReturn(cardPage);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        PageResponse<CardResponse> result = cardService.getPendingBlockRequests(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().blockRequested()).isTrue();
        verify(cardRepository).findByBlockRequestedTrueAndDeletedAtIsNull(pageable);
    }

    @Test
    void getMyCards_usesCurrentUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("50.00"));
        CardResponse cardResponse = new CardResponse(
                1L, "**** **** **** 1234", "john",
                card.getExpiryDate(), CardStatus.ACTIVE, card.getBalance(),
                false, Instant.now());
        Page<Card> cardPage = new PageImpl<>(List.of(card), pageable, 1);

        when(cardRepository.findUserActiveCards(eq(10L), isNull(), isNull(), eq(pageable)))
                .thenReturn(cardPage);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            PageResponse<CardResponse> result = cardService.getMyCards(null, null, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().owner()).isEqualTo("john");
            verify(cardRepository).findUserActiveCards(10L, null, null, pageable);
        }
    }

    @Test
    void getMyCard_returnsOwnedCard() {
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("75.00"));
        CardResponse cardResponse = new CardResponse(
                1L, "**** **** **** 1234", "john",
                card.getExpiryDate(), CardStatus.ACTIVE, card.getBalance(),
                false, Instant.now());

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            CardResponse result = cardService.getMyCard(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.owner()).isEqualTo("john");
        }
    }

    @Test
    void getMyCard_notFound_throwsNotFound() {
        when(cardRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> cardService.getMyCard(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Test
    void getMyCard_notOwner_throwsForbidden() {
        User other = TestFixtures.user(20L, "other");
        Card card = TestFixtures.activeCard(1L, other, new BigDecimal("10.00"));

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(TestFixtures.userPrincipal(10L, "john"));

            assertThatThrownBy(() -> cardService.getMyCard(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Test
    void block_setsStatusBlocked() {
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("10.00"));
        CardResponse cardResponse = new CardResponse(
                1L, "**** **** **** 1234", "john",
                card.getExpiryDate(), CardStatus.BLOCKED, card.getBalance(),
                false, Instant.now());

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.block(1L);

        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(card.isBlockRequested()).isFalse();
        assertThat(result.status()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void activate_setsStatusActive() {
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("10.00"));
        card.setStatus(CardStatus.BLOCKED);
        CardResponse cardResponse = new CardResponse(
                1L, "**** **** **** 1234", "john",
                card.getExpiryDate(), CardStatus.ACTIVE, card.getBalance(),
                false, Instant.now());

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.activate(1L);

        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(result.status()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void delete_marksCardDeleted() {
        Card card = TestFixtures.activeCard(1L, owner, new BigDecimal("10.00"));

        when(cardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        cardService.delete(1L);

        assertThat(card.isDeleted()).isTrue();
        assertThat(card.getDeletedAt()).isNotNull();
    }
}
