package com.edo.controller;

import com.edo.dto.request.PomodoroStartRequest;
import com.edo.dto.request.RegisterRequest;
import com.edo.entity.PomodoroSession;
import com.edo.entity.User;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PomodoroControllerTest {

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

    private String user1Token;
    private String user2Token;
    private User user1;
    private User user2;

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

    @Test
    @DisplayName("POST /api/v1/pomodoro/start - successfully starts an individual Pomodoro session")
    void startSession_Success() throws Exception {
        PomodoroStartRequest request = new PomodoroStartRequest(25, 5);

        mockMvc.perform(post("/api/v1/pomodoro/start")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", notNullValue()))
                .andExpect(jsonPath("$.type", is("INDIVIDUAL")))
                .andExpect(jsonPath("$.durationMinutes", is(25)))
                .andExpect(jsonPath("$.breakDurationMinutes", is(5)))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.startTime", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/start - returns 409 Conflict when user already has an active session")
    void startSession_ConflictWhenActiveSessionExists() throws Exception {
        PomodoroStartRequest request = new PomodoroStartRequest(25, 5);

        mockMvc.perform(post("/api/v1/pomodoro/start")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pomodoro/start")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("User already has an active Pomodoro session")));
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/start - returns 400 Bad Request on invalid durations")
    void startSession_InvalidDurations() throws Exception {
        PomodoroStartRequest request = new PomodoroStartRequest(0, 70);

        mockMvc.perform(post("/api/v1/pomodoro/start")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/start - returns 401 Unauthorized without auth token")
    void startSession_Unauthorized() throws Exception {
        PomodoroStartRequest request = new PomodoroStartRequest(25, 5);

        mockMvc.perform(post("/api/v1/pomodoro/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/{sessionId}/complete - successfully completes an active session")
    void completeSession_Success() throws Exception {
        PomodoroSession session = new PomodoroSession(
                user1,
                null,
                PomodoroSessionType.INDIVIDUAL,
                25,
                5,
                Instant.now().minus(25, ChronoUnit.MINUTES)
        );
        PomodoroSession savedSession = sessionRepository.save(session);

        mockMvc.perform(post("/api/v1/pomodoro/" + savedSession.getId() + "/complete")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(savedSession.getId().toString())))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.endTime", notNullValue()))
                .andExpect(jsonPath("$.earnedFocusMinutes", is(25)));
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/{sessionId}/complete - returns 409 Conflict if session is not in progress")
    void completeSession_AlreadyCompleted_Returns409() throws Exception {
        PomodoroSession session = new PomodoroSession(
                user1,
                null,
                PomodoroSessionType.INDIVIDUAL,
                25,
                5,
                Instant.now().minus(30, ChronoUnit.MINUTES)
        );
        session.setStatus(PomodoroSessionStatus.COMPLETED);
        session.setEndTime(Instant.now());
        PomodoroSession savedSession = sessionRepository.save(session);

        mockMvc.perform(post("/api/v1/pomodoro/" + savedSession.getId() + "/complete")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/{sessionId}/complete - returns 404 Not Found when session belongs to another user")
    void completeSession_OtherUserSession_Returns404() throws Exception {
        PomodoroSession session = new PomodoroSession(
                user1,
                null,
                PomodoroSessionType.INDIVIDUAL,
                25,
                5,
                Instant.now().minus(25, ChronoUnit.MINUTES)
        );
        PomodoroSession savedSession = sessionRepository.save(session);

        mockMvc.perform(post("/api/v1/pomodoro/" + savedSession.getId() + "/complete")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/{sessionId}/cancel - successfully cancels an active session")
    void cancelSession_Success() throws Exception {
        PomodoroSession session = new PomodoroSession(
                user1,
                null,
                PomodoroSessionType.INDIVIDUAL,
                25,
                5,
                Instant.now().minus(10, ChronoUnit.MINUTES)
        );
        PomodoroSession savedSession = sessionRepository.save(session);

        mockMvc.perform(post("/api/v1/pomodoro/" + savedSession.getId() + "/cancel")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(savedSession.getId().toString())))
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.endTime", notNullValue()))
                .andExpect(jsonPath("$.earnedFocusMinutes", is(0)));
    }

    @Test
    @DisplayName("POST /api/v1/pomodoro/{sessionId}/cancel - returns 404 Not Found when session belongs to another user")
    void cancelSession_OtherUserSession_Returns404() throws Exception {
        PomodoroSession session = new PomodoroSession(
                user1,
                null,
                PomodoroSessionType.INDIVIDUAL,
                25,
                5,
                Instant.now().minus(10, ChronoUnit.MINUTES)
        );
        PomodoroSession savedSession = sessionRepository.save(session);

        mockMvc.perform(post("/api/v1/pomodoro/" + savedSession.getId() + "/cancel")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/v1/pomodoro/sessions - returns paginated session history for current user")
    void getSessionHistory_Success() throws Exception {
        // User1 session 1
        PomodoroSession s1 = new PomodoroSession(user1, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now().minus(2, ChronoUnit.HOURS));
        s1.setStatus(PomodoroSessionStatus.COMPLETED);
        s1.setEndTime(Instant.now().minus(95, ChronoUnit.MINUTES));
        sessionRepository.save(s1);

        // User1 session 2
        PomodoroSession s2 = new PomodoroSession(user1, null, PomodoroSessionType.INDIVIDUAL, 30, 10, Instant.now().minus(1, ChronoUnit.HOURS));
        s2.setStatus(PomodoroSessionStatus.COMPLETED);
        s2.setEndTime(Instant.now().minus(30, ChronoUnit.MINUTES));
        sessionRepository.save(s2);

        // User2 session (should not be returned in User1 history)
        PomodoroSession sOther = new PomodoroSession(user2, null, PomodoroSessionType.INDIVIDUAL, 50, 10, Instant.now());
        sessionRepository.save(sOther);

        mockMvc.perform(get("/api/v1/pomodoro/sessions")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(s2.getId().toString())))
                .andExpect(jsonPath("$.content[1].id", is(s1.getId().toString())))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    @DisplayName("GET /api/v1/pomodoro/sessions - returns 400 Bad Request on invalid pagination params")
    void getSessionHistory_InvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/pomodoro/sessions")
                        .param("page", "-1")
                        .param("size", "0")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
