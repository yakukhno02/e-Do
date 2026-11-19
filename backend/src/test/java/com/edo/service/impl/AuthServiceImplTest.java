package com.edo.service.impl;

import com.edo.dto.request.ForgotPasswordRequest;
import com.edo.dto.request.LoginRequest;
import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.ResetPasswordRequest;
import com.edo.dto.response.AuthResponse;
import com.edo.dto.response.MessageResponse;
import com.edo.dto.response.RegisterResponse;
import com.edo.entity.PasswordResetToken;
import com.edo.entity.User;
import com.edo.enums.UserRole;
import com.edo.exception.BadRequestException;
import com.edo.exception.ConflictException;
import com.edo.exception.UnauthorizedException;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.UserRepository;
import com.edo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("user@example.com", "user", "Name", "encodedPassword", UserRole.USER);
        testUser.setId(userId);
    }

    @Test
    @DisplayName("register creates user, generates JWT token, and returns response")
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setUsername("user");
        request.setName("Name");
        request.setPassword("Password123!");
        request.setPasswordConfirm("Password123!");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });
        when(tokenProvider.generateToken(any(User.class))).thenReturn("jwt-token-123");

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("user", response.getUser().getUsername());
    }

    @Test
    @DisplayName("register throws BadRequestException when passwords do not match")
    void registerPasswordsMismatch() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("Password123!");
        request.setPasswordConfirm("DifferentPassword!");

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register throws ConflictException when email or username is already registered")
    void registerConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setUsername("user");
        request.setPassword("Password123!");
        request.setPasswordConfirm("Password123!");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login returns token when credentials are valid")
    void loginSuccess() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken(testUser)).thenReturn("jwt-token-123");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("user@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("login throws UnauthorizedException when password is incorrect")
    void loginWrongPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword!");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword!", "encodedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("forgotPassword generates token when user exists")
    void forgotPasswordUserExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        verify(resetTokenRepository).deleteAllByUserId(userId);
        verify(resetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("forgotPassword succeeds silently even if user email does not exist")
    void forgotPasswordUserDoesNotExist() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        verify(resetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword changes password and invalidates tokens")
    void resetPasswordSuccess() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setPassword("NewPassword123!");
        request.setPasswordConfirm("NewPassword123!");

        PasswordResetToken resetToken = new PasswordResetToken(testUser, "hash", Instant.now().plusSeconds(1800));

        when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedHash");

        authService.resetPassword(request);

        assertEquals("newEncodedHash", testUser.getPasswordHash());
        assertEquals(1, testUser.getCredentialsVersion());
        assertNotNull(resetToken.getUsedAt());
        verify(userRepository).save(testUser);
        verify(resetTokenRepository).save(resetToken);
    }

    @Test
    @DisplayName("resetPassword throws BadRequestException when token is expired")
    void resetPasswordExpiredToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setPassword("NewPassword123!");
        request.setPasswordConfirm("NewPassword123!");

        PasswordResetToken expiredToken = new PasswordResetToken(testUser, "hash", Instant.now().minusSeconds(60));

        when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredToken));

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any());
    }
}
