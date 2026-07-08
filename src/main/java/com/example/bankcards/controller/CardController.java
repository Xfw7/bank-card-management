package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("/admin/cards")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse create(@Valid @RequestBody CreateCardRequest request) {
        return cardService.create(request);
    }

    @GetMapping("/admin/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CardResponse> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String lastFour,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return cardService.getAll(userId, status, lastFour, pageable);
    }

    @GetMapping("/admin/cards/block-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CardResponse> getPendingBlockRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return cardService.getPendingBlockRequests(pageable);
    }

    @PatchMapping("/admin/cards/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse block(@PathVariable Long id) {
        return cardService.block(id);
    }

    @PatchMapping("/admin/cards/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse activate(@PathVariable Long id) {
        return cardService.activate(id);
    }

    @DeleteMapping("/admin/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        cardService.delete(id);
    }

    @GetMapping("/cards")
    @PreAuthorize("hasRole('USER')")
    public PageResponse<CardResponse> getMyCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String lastFour,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return cardService.getMyCards(status, lastFour, pageable);
    }

    @GetMapping("/cards/{id}")
    @PreAuthorize("hasRole('USER')")
    public CardResponse getMyCard(@PathVariable Long id) {
        return cardService.getMyCard(id);
    }

    @PatchMapping("/cards/{id}/block-request")
    @PreAuthorize("hasRole('USER')")
    public CardResponse requestBlock(@PathVariable Long id) {
        return cardService.requestBlock(id);
    }
}
