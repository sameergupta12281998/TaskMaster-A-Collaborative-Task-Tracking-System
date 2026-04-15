package com.taskmaster.service;

import com.taskmaster.dto.request.LoginRequest;
import com.taskmaster.dto.request.RefreshTokenRequest;
import com.taskmaster.dto.request.RegisterRequest;
import com.taskmaster.dto.response.AuthResponse;
import com.taskmaster.entity.RefreshToken;
import com.taskmaster.entity.User;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.exception.DuplicateResourceException;
import com.taskmaster.repository.RefreshTokenRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.security.JwtTokenProvider;
import com.taskmaster.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Test@1234")
                .fullName("Test User")
                .build();

        savedUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .fullName("Test User")
                .build();
        savedUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@1234")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshTokenValue()).thenReturn("refresh_token_value");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void register_DuplicateUsername() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_DuplicateEmail() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Test@1234")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshTokenValue()).thenReturn("refresh_token_value");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token_value");
    }

    @Test
    @DisplayName("Should throw when login user is not found")
    void login_UserNotFound() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("missing")
                .password("Test@1234")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("old_refresh").build();

        RefreshToken oldToken = RefreshToken.builder()
                .token("old_refresh")
                .user(savedUser)
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndRevokedFalse("old_refresh"))
                .thenReturn(Optional.of(oldToken));
        when(jwtTokenProvider.generateAccessToken(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail()))
                .thenReturn("new_access_token");
        when(jwtTokenProvider.generateRefreshTokenValue()).thenReturn("new_refresh_token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
        assertThat(oldToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Should throw when refresh token is invalid")
    void refreshToken_InvalidToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("missing").build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("Should throw when refresh token is expired")
    void refreshToken_ExpiredToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("expired").build();

        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired")
                .user(savedUser)
                .expiryDate(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndRevokedFalse("expired"))
                .thenReturn(Optional.of(expiredToken));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");

        assertThat(expiredToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Should revoke refresh token on logout")
    void logout_RevokeToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh")
                .user(savedUser)
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.logout("refresh");

        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    @DisplayName("Should do nothing on logout if token is missing")
    void logout_MissingToken() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString())).thenReturn(Optional.empty());

        authService.logout("missing");

        verify(refreshTokenRepository, never()).save(any());
    }
}
