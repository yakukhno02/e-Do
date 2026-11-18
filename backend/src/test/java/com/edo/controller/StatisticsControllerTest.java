package com.edo.controller;

import com.edo.dto.request.RegisterRequest;
import com.edo.entity.PomodoroSession;
import com.edo.entity.Room;
import com.edo.entity.User;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PomodoroSessionRepository sessionRepository;

    @Autowired
    private ProductivityGoalRepository goalRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    private String user1Token;
    private String user2Token;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

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
        user1Token = objectMapper.readTree(res1).get("token").asText();
        user1 = userRepository.findByEmail("jane@example.com").orElseThrow();

        RegisterRequest secondUser = new RegisterRequest(
                "John Smith",
                "johnsmith",
                "john@example.com",
                "Password123!",
                "Password123!"
        );
        String res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        user2Token = objectMapper.readTree(res2).get("token").asText();
        user2 = userRepository.findByEmail("john@example.com").orElseThrow();
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
    @DisplayName("GET /api/v1/statistics/summary - returns aggregated statistics and recent sessions")
    void getSummary_Success() throws Exception {
        // Individual completed session (25 min)
        PomodoroSession s1 = new PomodoroSession(user1, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now().minus(3, ChronoUnit.HOURS));
        s1.setStatus(PomodoroSessionStatus.COMPLETED);
        s1.setEndTime(Instant.now().minus(155, ChronoUnit.MINUTES));
        sessionRepository.save(s1);

        // Shared completed session (30 min)
        Room room = roomRepository.save(new Room("CODE01", "Study Room", user1, 30, 5));
        PomodoroSession s2 = new PomodoroSession(user1, room, PomodoroSessionType.SHARED, 30, 5, Instant.now().minus(2, ChronoUnit.HOURS));
        s2.setStatus(PomodoroSessionStatus.COMPLETED);
        s2.setEndTime(Instant.now().minus(90, ChronoUnit.MINUTES));
        sessionRepository.save(s2);

        // Cancelled session (should not count towards completed pomodoros or focus minutes)
        PomodoroSession s3 = new PomodoroSession(user1, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now().minus(1, ChronoUnit.HOURS));
        s3.setStatus(PomodoroSessionStatus.CANCELLED);
        s3.setEndTime(Instant.now().minus(50, ChronoUnit.MINUTES));
        sessionRepository.save(s3);

        mockMvc.perform(get("/api/v1/statistics/summary")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedPomodoros", is(2)))
                .andExpect(jsonPath("$.totalFocusMinutes", is(55)))
                .andExpect(jsonPath("$.totalSharedSessions", is(1)))
                .andExpect(jsonPath("$.recentSessions", hasSize(3)))
                .andExpect(jsonPath("$.recentSessions[0].id", is(s3.getId().toString())))
                .andExpect(jsonPath("$.recentSessions[1].id", is(s2.getId().toString())))
                .andExpect(jsonPath("$.recentSessions[2].id", is(s1.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/v1/statistics/summary - enforces data isolation between users (NFR-06)")
    void getSummary_UserIsolation() throws Exception {
        // User 2 completed session (50 min)
        PomodoroSession sOther = new PomodoroSession(user2, null, PomodoroSessionType.INDIVIDUAL, 50, 10, Instant.now().minus(1, ChronoUnit.HOURS));
        sOther.setStatus(PomodoroSessionStatus.COMPLETED);
        sOther.setEndTime(Instant.now().minus(10, ChronoUnit.MINUTES));
        sessionRepository.save(sOther);

        // User 1 requests stats - should be 0
        mockMvc.perform(get("/api/v1/statistics/summary")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedPomodoros", is(0)))
                .andExpect(jsonPath("$.totalFocusMinutes", is(0)))
                .andExpect(jsonPath("$.totalSharedSessions", is(0)))
                .andExpect(jsonPath("$.recentSessions", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/statistics/summary - returns 401 Unauthorized without token")
    void getSummary_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/summary"))
                .andExpect(status().isUnauthorized());
    }
}
