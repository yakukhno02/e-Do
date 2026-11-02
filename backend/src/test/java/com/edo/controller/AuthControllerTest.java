package com.edo.controller;

import com.edo.dto.request.ForgotPasswordRequest;
import com.edo.dto.request.LoginRequest;
import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.ResetPasswordRequest;
import com.edo.entity.PasswordResetToken;
import com.edo.entity.User;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @BeforeEach
    void setUp() {
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /register - successful registration returns 201 Created and valid token")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Назар Цебринський",
                "nazarii_dev",
                "nazar@example.com",
                "StrongPassword123!",
                "StrongPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name", is("Назар Цебринський")))
                .andExpect(jsonPath("$.user.username", is("nazarii_dev")))
                .andExpect(jsonPath("$.user.email", is("nazar@example.com")))
                .andExpect(jsonPath("$.user.role", is("USER")));

        assertTrue(userRepository.existsByEmail("nazar@example.com"));
    }

    @Test
    @DisplayName("POST /register - password and passwordConfirm mismatch returns 400 Bad Request")
    void register_PasswordMismatch_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Назар",
                "nazarii_dev",
                "nazar@example.com",
                "StrongPassword123!",
                "DifferentPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("do not match")));
    }

    @Test
    @DisplayName("POST /register - duplicate email or username returns 409 Conflict")
    void register_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest first = new RegisterRequest(
                "Перший",
                "user_one",
                "same@example.com",
                "Password123!",
                "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest(
                "Другий",
                "user_two",
                "same@example.com",
                "Password123!",
                "Password123!"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /login - successful credentials returns 200 OK and token")
    void login_Success() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Тестовий",
                "test_user",
                "login@example.com",
                "Password123!",
                "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("login@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email", is("login@example.com")));
    }

    @Test
    @DisplayName("POST /login - invalid password returns 401 Unauthorized")
    void login_InvalidPassword_Returns401() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Тестовий",
                "test_user2",
                "login2@example.com",
                "Password123!",
                "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("login2@example.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /forgot-password - unknown email still returns 200 OK (anti-enumeration protection)")
    void forgotPassword_UnknownEmail_Returns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password reset instructions")));
    }

    @Test
    @DisplayName("POST /reset-password - password reset increments credentialsVersion and invalidates old password")
    void resetPassword_Success_InvalidatesOldCredentials() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Reset User",
                "reset_user",
                "reset@example.com",
                "OldPassword123!",
                "OldPassword123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("reset@example.com"))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("reset@example.com").orElseThrow();
        int initialCredentialsVersion = user.getCredentialsVersion();

        PasswordResetToken tokenEntity = resetTokenRepository.findAll().get(0);
        assertFalse(tokenEntity.getTokenHash().isEmpty());

        String rawToken = "my-secret-test-token-12345";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        String testHash = HexFormat.of().formatHex(hash);

        tokenEntity.setTokenHash(testHash);
        resetTokenRepository.save(tokenEntity);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(
                rawToken,
                "NewPassword123!",
                "NewPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("reset@example.com", "OldPassword123!"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("reset@example.com", "NewPassword123!"))))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findByEmail("reset@example.com").orElseThrow();
        assertTrue(updatedUser.getCredentialsVersion() > initialCredentialsVersion);
    }

    @Test
    @DisplayName("POST /logout - authenticated request returns 204 No Content, unauthenticated returns 401")
    void logout_Returns204() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Logout User",
                "logout_user",
                "logout@example.com",
                "Password123!",
                "Password123!"
        );
        String responseContent = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseContent).get("token").asText();

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
