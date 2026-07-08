package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        Long userId = SecurityUtils.getCurrentUser().id();

        if (request.fromCardId().equals(request.toCardId())) {
            throw new BusinessException(ErrorCode.SAME_CARD_TRANSFER);
        }

        Long firstId = Math.min(request.fromCardId(), request.toCardId());
        Long secondId = Math.max(request.fromCardId(), request.toCardId());

        Card first = lockCard(firstId);
        Card second = lockCard(secondId);

        Card fromCard = first.getId().equals(request.fromCardId()) ? first : second;
        Card toCard = first.getId().equals(request.toCardId()) ? first : second;

        assertOwnership(fromCard, userId);
        assertOwnership(toCard, userId);
        assertCardUsable(fromCard);
        assertCardUsable(toCard);

        if (!fromCard.hasSufficientBalance(request.amount())) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        fromCard.debit(request.amount());
        toCard.credit(request.amount());

        Transfer transfer = Transfer.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.amount())
                .build();

        return transferMapper.toResponse(transferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getMyTransfers(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUser().id();
        return PageResponse.from(
                transferRepository.findByUserId(userId, pageable)
                        .map(transferMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getMyCardTransfers(Long cardId, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUser().id();
        return PageResponse.from(
                transferRepository.findByCardIdAndUserId(cardId, userId, pageable)
                        .map(transferMapper::toResponse)
        );
    }

    private Card lockCard(Long id) {
        return cardRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Card not found: " + id));
    }

    private void assertOwnership(Card card, Long userId) {
        if (!card.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Card does not belong to user: " + userId);
        }
    }

    private void assertCardUsable(Card card) {
        if (!card.canBeUsedForTransfer(LocalDate.now())) {
            if (card.getStatus() == CardStatus.BLOCKED) {
                throw new BusinessException(ErrorCode.CARD_BLOCKED);
            }
            throw new BusinessException(ErrorCode.CARD_EXPIRED);
        }
    }
}
