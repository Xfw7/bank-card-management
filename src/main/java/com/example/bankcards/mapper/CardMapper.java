package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(target = "owner", source = "user.username")
    @Mapping(target = "maskedNumber", expression = "java(maskPan(card.getLastFour()))")
    CardResponse toResponse(Card card);

    List<CardResponse> toResponseList(List<Card> cards);

    default Page<CardResponse> toResponsePage(Page<Card> cards) {
        return cards.map(this::toResponse);
    }

    default String maskPan(String lastFour) {
        return "**** **** **** " + lastFour;
    }
}
