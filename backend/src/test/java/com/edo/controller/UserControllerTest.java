package com.edo.controller;

import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.UpdateProfileRequest;
import com.edo.entity.PomodoroSession;
import com.edo.entity.ProductivityGoal;
import com.edo.entity.User;
import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.TaskRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private PomodoroSessionRepository sessionRepository;

    @Autowired
    private ProductivityGoalRepository goalRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        sessionRepository.deleteAll();
        goalRepository.deleteAll();
        taskRepository.deleteAll();
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest firstUser = new RegisterRequest(
                "Jane Doe",
                "janedoe",
                "jane@example.com",
                "Password123!",
                "Password123!"
        );
        String res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(res1).get("token").asText();

        RegisterRequest secondUser = new RegisterRequest(
                "John Smith",
                "johnsmith",
                "john@example.com",
                "Password123!",
                "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUser)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/v1/users/me - returns profile with statistics and goals")
    void getProfile_Success() throws Exception {
        User user = userRepository.findByEmail("jane@example.com").orElseThrow();

        PomodoroSession session = new PomodoroSession(
                user,
                null,
                PomodoroSessionType.INDIVIDUAL,
                25,
                5,
                Instant.now().minus(25, ChronoUnit.MINUTES)
        );
        session.setStatus(PomodoroSessionStatus.COMPLETED);
        session.setEndTime(Instant.now());
        sessionRepository.save(session);

        ProductivityGoal goal = new ProductivityGoal(user, GoalPeriod.DAILY, GoalType.POMODORO_COUNT, 4);
        goalRepository.save(goal);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("jane@example.com")))
                .andExpect(jsonPath("$.username", is("janedoe")))
                .andExpect(jsonPath("$.completedPomodoros", is(1)))
                .andExpect(jsonPath("$.totalFocusMinutes", is(25)))
                .andExpect(jsonPath("$.goals.daily", hasSize(1)))
                .andExpect(jsonPath("$.goals.daily[0].currentValue", is(1)))
                .andExpect(jsonPath("$.goals.daily[0].progressPercentage", is(25.0)));
    }

    @Test
    @DisplayName("GET /api/v1/users/me - unauthorized when no token provided")
    void getProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - successfully updates name and username")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Updated", "jane_updated");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jane Updated")))
                .andExpect(jsonPath("$.username", is("jane_updated")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - duplicate username returns 409 Conflict")
    void updateProfile_DuplicateUsername_Returns409() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Updated", "johnsmith");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }
}
