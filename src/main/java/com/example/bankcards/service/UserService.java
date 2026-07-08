package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Username already taken: " + request.username());
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAll(Pageable pageable) {
        return PageResponse.from(
                userRepository.findAllByOrderByCreatedAtDesc(pageable)
                        .map(userMapper::toResponse)
        );
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findById(id);

        if (request.enabled() != null) {
            if (request.enabled()) {
                user.enable();
            } else {
                user.disable();
            }
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        return userMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(findById(id));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found: " + id));
    }
}
