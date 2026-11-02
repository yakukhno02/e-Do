package com.edo.service.impl;

import com.edo.dto.request.ForgotPasswordRequest;
import com.edo.dto.request.LoginRequest;
import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.ResetPasswordRequest;
import com.edo.dto.response.AuthResponse;
import com.edo.dto.response.MessageResponse;
import com.edo.dto.response.RegisterResponse;
import com.edo.dto.response.UserSummaryResponse;
import com.edo.entity.PasswordResetToken;
import com.edo.entity.User;
import com.edo.enums.UserRole;
import com.edo.exception.BadRequestException;
import com.edo.exception.ConflictException;
import com.edo.exception.UnauthorizedException;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.UserRepository;
import com.edo.security.JwtTokenProvider;
import com.edo.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordResetTokenRepository resetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BadRequestException("Password and password confirmation do not match");
        }

        if (userRepository.existsByEmail(request.getEmail()) || userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username or email already exists");
        }

        User user = new User(
                request.getEmail(),
                request.getUsername(),
                request.getName(),
                passwordEncoder.encode(request.getPassword()),
                UserRole.USER
        );

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser);

        UserSummaryResponse summary = new UserSummaryResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return new RegisterResponse(savedUser.getId(), token, summary);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = tokenProvider.generateToken(user);
        UserSummaryResponse summary = new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(token, summary);
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = hashToken(rawToken);

            resetTokenRepository.deleteAllByUserId(user.getId());

            PasswordResetToken resetToken = new PasswordResetToken(
                    user,
                    tokenHash,
                    Instant.now().plus(30, ChronoUnit.MINUTES)
            );
            resetTokenRepository.save(resetToken);

            log.info("Password reset token generated for user: {}", user.getId());
        });

        return new MessageResponse("Password reset instructions have been sent to your email.");
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BadRequestException("Password and password confirmation do not match");
        }

        String tokenHash = hashToken(request.getToken());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // Збільшення credentialsVersion інвалідує всі попередні JWT токени (U-01)
        user.setCredentialsVersion(user.getCredentialsVersion() + 1);
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        resetTokenRepository.save(resetToken);
        resetTokenRepository.deleteAllByUserId(user.getId());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
