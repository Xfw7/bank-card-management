package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "fromCardId", source = "fromCard.id")
    @Mapping(target = "toCardId", source = "toCard.id")
    TransferResponse toResponse(Transfer transfer);

    List<TransferResponse> toResponseList(List<Transfer> transfers);

    default Page<TransferResponse> toResponsePage(Page<Transfer> transfers) {
        return transfers.map(this::toResponse);
    }
}
