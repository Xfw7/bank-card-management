package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.SecurityUtils;
import com.example.bankcards.util.AesEncryptor;
import com.example.bankcards.util.CardMaskUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;
    private final AesEncryptor aesEncryptor;


    @Transactional
    public CardResponse create(CreateCardRequest request) {
        var owner = userRepository.findByUsername(request.ownerUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "User not found: " + request.ownerUsername()));

        String normalized = CardMaskUtil.normalize(request.pan());
        String encryptedPan = aesEncryptor.encrypt(normalized);
        String lastFour = CardMaskUtil.extractLastFour(normalized);

        Card card = Card.builder()
                .user(owner)
                .encryptedPan(encryptedPan)
                .lastFour(lastFour)
                .expiryDate(request.expiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.balance())
                .build();

        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getAll(Long userId, CardStatus status, String lastFour, Pageable pageable) {
        return PageResponse.from(
                cardRepository.findAllActiveCards(userId, status, lastFour, pageable)
                        .map(cardMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getPendingBlockRequests(Pageable pageable) {
        return PageResponse.from(
                cardRepository.findByBlockRequestedTrueAndDeletedAtIsNull(pageable)
                        .map(cardMapper::toResponse)
        );
    }

    @Transactional
    public CardResponse block(Long id) {
        Card card = findActive(id);
        card.block();
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse activate(Long id) {
        Card card = findActive(id);
        card.activate();
        return cardMapper.toResponse(card);
    }

    @Transactional
    public void delete(Long id) {
        Card card = findActive(id);
        card.markDeleted();
    }



    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getMyCards(CardStatus status, String lastFour, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUser().id();
        return PageResponse.from(
                cardRepository.findUserActiveCards(userId, status, lastFour, pageable)
                        .map(cardMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public CardResponse getMyCard(Long id) {
        Long userId = SecurityUtils.getCurrentUser().id();
        Card card = findActive(id);
        assertOwnership(card, userId);
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse requestBlock(Long id) {
        Long userId = SecurityUtils.getCurrentUser().id();
        Card card = findActive(id);
        assertOwnership(card, userId);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.CARD_BLOCKED);
        }
        card.requestBlock();
        return cardMapper.toResponse(card);
    }

    private Card findActive(Long id) {
        return cardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Card not found: " + id));
    }

    private void assertOwnership(Card card, Long userId) {
        if (!card.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Card does not belong to user: " + userId);
        }
    }

}
