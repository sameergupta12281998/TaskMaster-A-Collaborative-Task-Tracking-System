package com.taskmaster.service;

import com.taskmaster.dto.request.UserUpdateRequest;
import com.taskmaster.dto.response.UserResponse;
import com.taskmaster.entity.User;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .fullName("Test User")
                .bio("Bio")
                .build();
        user.setId(userId);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should get current user successfully")
    void getCurrentUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(userId);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should throw when user not found")
    void getCurrentUser_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("Should update user fullName and bio")
    void updateUser_Success() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .bio("Updated Bio")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(userId, request);

        assertThat(response.getFullName()).isEqualTo("Updated Name");
        assertThat(response.getBio()).isEqualTo("Updated Bio");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should ignore blank fullName update")
    void updateUser_BlankFullNameIgnored() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("  ")
                .bio("Updated Bio")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(userId, request);

        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getBio()).isEqualTo("Updated Bio");
    }
}
