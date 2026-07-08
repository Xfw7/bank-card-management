package com.example.bankcards.config;

import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardExpiryScheduler {

    private final CardService cardService;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void markExpiredCards() {
        int count = cardService.markExpiredCards();
        if (count > 0) {
            log.info("Marked {} card(s) as expired", count);
        }
    }
}
