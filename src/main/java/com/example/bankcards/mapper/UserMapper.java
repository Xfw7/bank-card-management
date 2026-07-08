package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    default Page<UserResponse> toResponsePage(Page<User> users) {
        return users.map(this::toResponse);
    }
}
