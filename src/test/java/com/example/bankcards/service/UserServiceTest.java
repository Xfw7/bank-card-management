package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void create_persistsEncodedUser() {
        CreateUserRequest request = new CreateUserRequest("john", "password123", Role.USER);
        UserResponse response = new UserResponse(1L, "john", Role.USER, true,
                Instant.parse("2026-07-09T12:00:00Z"));

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result.username()).isEqualTo("john");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void create_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest("john", "password123", Role.USER);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);

        verify(userRepository, never()).save(any());
    }

    @Test
    void getById_returnsUser() {
        User user = TestFixtures.user(1L, "john");
        UserResponse response = new UserResponse(1L, "john", Role.USER, true,
                Instant.parse("2026-07-09T12:00:00Z"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getById(1L);

        assertThat(result.username()).isEqualTo("john");
        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void getById_missingUser_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void delete_removesExistingUser() {
        User user = TestFixtures.user(1L, "john");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_missingUser_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        verify(userRepository, never()).delete(any());
    }

    @Test
    void getAll_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = TestFixtures.user(1L, "john");
        UserResponse response = new UserResponse(1L, "john", Role.USER, true,
                Instant.parse("2026-07-09T12:00:00Z"));
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(response);

        PageResponse<UserResponse> result = userService.getAll(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().username()).isEqualTo("john");
        assertThat(result.totalElements()).isEqualTo(1);
        verify(userRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void update_disablesUser() {
        User user = TestFixtures.user(1L, "john");
        UserResponse response = new UserResponse(1L, "john", Role.USER, false,
                Instant.parse("2026-07-09T12:00:00Z"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(1L, new UpdateUserRequest(false, null));

        assertThat(user.isEnabled()).isFalse();
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void update_enablesUser() {
        User user = TestFixtures.user(1L, "john");
        user.disable();
        UserResponse response = new UserResponse(1L, "john", Role.USER, true,
                Instant.parse("2026-07-09T12:00:00Z"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(1L, new UpdateUserRequest(true, null));

        assertThat(user.isEnabled()).isTrue();
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void update_changesRole() {
        User user = TestFixtures.user(1L, "john");
        UserResponse response = new UserResponse(1L, "john", Role.ADMIN, true,
                Instant.parse("2026-07-09T12:00:00Z"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(1L, new UpdateUserRequest(null, Role.ADMIN));

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void update_missingUser_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, new UpdateUserRequest(true, Role.ADMIN)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
