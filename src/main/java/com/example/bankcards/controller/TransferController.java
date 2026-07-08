package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transferService.transfer(request);
    }

    @GetMapping
    public PageResponse<TransferResponse> getMyTransfers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transferService.getMyTransfers(pageable);
    }

    @GetMapping("/cards/{cardId}")
    public PageResponse<TransferResponse> getMyCardTransfers(
            @PathVariable Long cardId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transferService.getMyCardTransfers(cardId, pageable);
    }
}
