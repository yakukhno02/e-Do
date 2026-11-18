package com.edo.controller;

import com.edo.dto.request.ForgotPasswordRequest;
import com.edo.dto.request.LoginRequest;
import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.ResetPasswordRequest;
import com.edo.entity.PasswordResetToken;
import com.edo.entity.User;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.RoomParticipantRepository;
import com.edo.repository.RoomRepository;
import com.edo.repository.TaskRepository;
import com.edo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import static org.hamcrest.Matchers.*;
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

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PomodoroSessionRepository sessionRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ProductivityGoalRepository goalRepository;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        sessionRepository.deleteAll();
        roomParticipantRepository.deleteAll();
        roomRepository.deleteAll();
        goalRepository.deleteAll();
        taskRepository.deleteAll();
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
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.name", is("Назар Цебринський")))
                .andExpect(jsonPath("$.user.username", is("nazarii_dev")))
                .andExpect(jsonPath("$.user.email", is("nazar@example.com")))
                .andExpect(jsonPath("$.user.role", is("USER")));

        assertTrue(userRepository.existsByEmail("nazar@example.com"));
    }

    @Test
    @DisplayName("POST /register - validation failure returns 400 Bad Request")
    void register_ValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "invalid user!",
                "not-an-email",
                "short",
                "mismatch"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("POST /register - duplicate email returns 409 Conflict")
    void register_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest request1 = new RegisterRequest(
                "First User", "first_user", "duplicate@example.com", "Password123!", "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        RegisterRequest request2 = new RegisterRequest(
                "Second User", "second_user", "duplicate@example.com", "Password123!", "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("POST /login - successful login returns 200 OK and valid JWT token")
    void login_Success() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Nazar", "nazar_login", "login@example.com", "Password123!", "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("login@example.com", "Password123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("login@example.com")));
    }

    @Test
    @DisplayName("POST /login - invalid credentials returns 401 Unauthorized")
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest login = new LoginRequest("unknown@example.com", "WrongPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /forgot-password - returns 200 OK for both existing and non-existing email")
    void forgotPassword_AlwaysReturns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("anyone@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password reset instructions")));
    }

    @Test
    @DisplayName("POST /reset-password - resets password successfully and invalidates tokens")
    void resetPassword_Success() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Nazar", "nazar_reset", "reset@example.com", "OldPassword123!", "OldPassword123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("reset@example.com").orElseThrow();

        String rawToken = "my-secret-test-token-1234567890";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        String tokenHash = HexFormat.of().formatHex(hashBytes);

        PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, Instant.now().plus(30, ChronoUnit.MINUTES));
        resetTokenRepository.save(resetToken);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(
                rawToken, "NewPassword123!", "NewPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isNoContent());

        LoginRequest oldLogin = new LoginRequest("reset@example.com", "OldPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        LoginRequest newLogin = new LoginRequest("reset@example.com", "NewPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /reset-password - invalid token returns 400 Bad Request")
    void resetPassword_InvalidToken_Returns400() throws Exception {
        ResetPasswordRequest resetRequest = new ResetPasswordRequest(
                "invalid-token", "NewPassword123!", "NewPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
