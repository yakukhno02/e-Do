package com.edo.controller;

import com.edo.dto.request.GoalUpsertRequest;
import com.edo.dto.request.RegisterRequest;
import com.edo.entity.PomodoroSession;
import com.edo.entity.User;
import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductivityGoalRepository goalRepository;

    @Autowired
    private PomodoroSessionRepository sessionRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    private String userToken;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        RegisterRequest registerRequest = new RegisterRequest(
                "Jane Doe",
                "janedoe",
                "jane@example.com",
                "Password123!",
                "Password123!"
        );
        String res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        userToken = objectMapper.readTree(res).get("token").asText();
        user = userRepository.findByEmail("jane@example.com").orElseThrow();
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
    @DisplayName("POST /api/v1/goals - successfully creates new goal and calculates progress")
    void upsertGoal_Create_Success() throws Exception {
        // Complete one session today
        PomodoroSession session = new PomodoroSession(
                user, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now().minus(25, ChronoUnit.MINUTES)
        );
        session.setStatus(PomodoroSessionStatus.COMPLETED);
        session.setEndTime(Instant.now());
        sessionRepository.save(session);

        GoalUpsertRequest request = new GoalUpsertRequest(GoalPeriod.DAILY, GoalType.POMODORO_COUNT, 4);

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.period", is("DAILY")))
                .andExpect(jsonPath("$.type", is("POMODORO_COUNT")))
                .andExpect(jsonPath("$.targetValue", is(4)))
                .andExpect(jsonPath("$.currentValue", is(1)))
                .andExpect(jsonPath("$.progressPercentage", is(25.0)));

        assertEquals(1, goalRepository.findAllByUserId(user.getId()).size());
    }

    @Test
    @DisplayName("POST /api/v1/goals - upsert updates targetValue of existing goal")
    void upsertGoal_Update_Success() throws Exception {
        GoalUpsertRequest firstRequest = new GoalUpsertRequest(GoalPeriod.WEEKLY, GoalType.FOCUS_MINUTES, 500);

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetValue", is(500)));

        GoalUpsertRequest secondRequest = new GoalUpsertRequest(GoalPeriod.WEEKLY, GoalType.FOCUS_MINUTES, 600);

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetValue", is(600)));

        // Still only 1 goal exists for this period and type
        assertEquals(1, goalRepository.findAllByUserId(user.getId()).size());
    }

    @Test
    @DisplayName("POST /api/v1/goals - returns 400 Bad Request on invalid targetValue")
    void upsertGoal_InvalidTargetValue() throws Exception {
        GoalUpsertRequest request = new GoalUpsertRequest(GoalPeriod.DAILY, GoalType.POMODORO_COUNT, 0);

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/goals - returns 401 Unauthorized without token")
    void upsertGoal_Unauthorized() throws Exception {
        GoalUpsertRequest request = new GoalUpsertRequest(GoalPeriod.DAILY, GoalType.POMODORO_COUNT, 5);

        mockMvc.perform(post("/api/v1/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
