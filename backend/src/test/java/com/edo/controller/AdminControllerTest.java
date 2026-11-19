package com.edo.controller;

import com.edo.dto.request.LoginRequest;
import com.edo.entity.PomodoroSession;
import com.edo.entity.ProductivityGoal;
import com.edo.entity.Room;
import com.edo.entity.RoomParticipant;
import com.edo.entity.Task;
import com.edo.entity.User;
import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;
import com.edo.enums.UserRole;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.RoomParticipantRepository;
import com.edo.repository.RoomRepository;
import com.edo.repository.TaskRepository;
import com.edo.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

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
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ProductivityGoalRepository goalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private String loginAndGetToken(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("token").asText();
    }

    private User createAndSaveUser(String email, String username, String name, UserRole role) {
        User user = new User(email, username, name, passwordEncoder.encode("Password123!"), role);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Admin can get paginated list of all users with statistics")
    void getUsersAsAdminSuccess() throws Exception {
        User admin = createAndSaveUser("admin@example.com", "admin_user", "Admin", UserRole.ADMIN);
        User user = createAndSaveUser("user@example.com", "regular_user", "Regular", UserRole.USER);

        // Add a completed session for the regular user
        PomodoroSession session = new PomodoroSession(
                user, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now().minusSeconds(1800)
        );
        session.setStatus(PomodoroSessionStatus.COMPLETED);
        session.setEndTime(Instant.now().minusSeconds(300));
        sessionRepository.save(session);

        String adminToken = loginAndGetToken("admin@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("Regular user receives 403 Forbidden when requesting admin users list")
    void getUsersAsRegularUserForbidden() throws Exception {
        createAndSaveUser("user@example.com", "regular_user", "Regular", UserRole.USER);
        String userToken = loginAndGetToken("user@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to admin users list receives 401 Unauthorized")
    void getUsersUnauthenticatedUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin requesting users list with invalid pagination receives 400 Bad Request")
    void getUsersInvalidPagination() throws Exception {
        createAndSaveUser("admin@example.com", "admin_user", "Admin", UserRole.ADMIN);
        String adminToken = loginAndGetToken("admin@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin can delete an existing user and all their associated records")
    void deleteUserAsAdminSuccess() throws Exception {
        User admin = createAndSaveUser("admin@example.com", "admin_user", "Admin", UserRole.ADMIN);
        User user = createAndSaveUser("target@example.com", "target_user", "Target", UserRole.USER);

        // Associated task
        Task task = new Task(user, "User task", "Description", null, TaskStatus.TODO, TaskPriority.MEDIUM);
        taskRepository.save(task);

        // Associated goal
        ProductivityGoal goal = new ProductivityGoal(user, GoalPeriod.DAILY, GoalType.POMODORO_COUNT, 8);
        goalRepository.save(goal);

        // Associated session
        PomodoroSession session = new PomodoroSession(
                user, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now()
        );
        sessionRepository.save(session);

        // Associated room
        Room room = new Room("DEL123", "User Room", user, 25, 5);
        roomRepository.save(room);
        roomParticipantRepository.save(new RoomParticipant(room, user));

        String adminToken = loginAndGetToken("admin@example.com", "Password123!");

        mockMvc.perform(delete("/api/v1/admin/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(user.getId()));
        assertFalse(taskRepository.existsById(task.getId()));
        assertFalse(goalRepository.existsById(goal.getId()));
        assertFalse(sessionRepository.existsById(session.getId()));
        assertFalse(roomRepository.existsById(room.getId()));
    }

    @Test
    @DisplayName("Admin deleting non-existent user receives 404 Not Found")
    void deleteUserNotFound() throws Exception {
        createAndSaveUser("admin@example.com", "admin_user", "Admin", UserRole.ADMIN);
        String adminToken = loginAndGetToken("admin@example.com", "Password123!");

        mockMvc.perform(delete("/api/v1/admin/users/{userId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Regular user receives 403 Forbidden when attempting to delete a user")
    void deleteUserAsRegularUserForbidden() throws Exception {
        createAndSaveUser("user1@example.com", "user1", "User 1", UserRole.USER);
        User user2 = createAndSaveUser("user2@example.com", "user2", "User 2", UserRole.USER);

        String user1Token = loginAndGetToken("user1@example.com", "Password123!");

        mockMvc.perform(delete("/api/v1/admin/users/{userId}", user2.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can get paginated list of all created rooms")
    void getRoomsAsAdminSuccess() throws Exception {
        User admin = createAndSaveUser("admin@example.com", "admin_user", "Admin", UserRole.ADMIN);
        User user = createAndSaveUser("creator@example.com", "creator_user", "Creator", UserRole.USER);

        Room room = new Room("ADM999", "Admin Visible Room", user, 30, 5);
        roomRepository.save(room);
        roomParticipantRepository.save(new RoomParticipant(room, user));

        String adminToken = loginAndGetToken("admin@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].code", is("ADM999")))
                .andExpect(jsonPath("$.content[0].name", is("Admin Visible Room")))
                .andExpect(jsonPath("$.content[0].participantsCount", is(1)));
    }

    @Test
    @DisplayName("Regular user receives 403 Forbidden when requesting admin rooms list")
    void getRoomsAsRegularUserForbidden() throws Exception {
        createAndSaveUser("user@example.com", "regular_user", "Regular", UserRole.USER);
        String userToken = loginAndGetToken("user@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/admin/rooms")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can force delete a room")
    void deleteRoomAsAdminSuccess() throws Exception {
        User admin = createAndSaveUser("admin@example.com", "admin_user", "Admin", UserRole.ADMIN);
        User creator = createAndSaveUser("creator@example.com", "creator_user", "Creator", UserRole.USER);

        Room room = new Room("RM4001", "Room To Delete", creator, 25, 5);
        roomRepository.save(room);
        roomParticipantRepository.save(new RoomParticipant(room, creator));

        // Shared session referencing this room
        PomodoroSession session = new PomodoroSession(
                creator, room, PomodoroSessionType.SHARED, 25, 5, Instant.now()
        );
        sessionRepository.save(session);

        String adminToken = loginAndGetToken("admin@example.com", "Password123!");

        mockMvc.perform(delete("/api/v1/admin/rooms/{roomId}", room.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(roomRepository.existsById(room.getId()));
    }

    @Test
    @DisplayName("Admin deleting non-existent room receives 404 Not Found")
    void deleteRoomNotFound() throws Exception {
        createAndSaveUser("admin@example.com", "admin_user", "Admin", UserRole.ADMIN);
        String adminToken = loginAndGetToken("admin@example.com", "Password123!");

        mockMvc.perform(delete("/api/v1/admin/rooms/{roomId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Regular user receives 403 Forbidden when attempting to delete a room")
    void deleteRoomAsRegularUserForbidden() throws Exception {
        User creator = createAndSaveUser("creator@example.com", "creator_user", "Creator", UserRole.USER);
        Room room = new Room("RM4002", "Protected Room", creator, 25, 5);
        roomRepository.save(room);

        String creatorToken = loginAndGetToken("creator@example.com", "Password123!");

        mockMvc.perform(delete("/api/v1/admin/rooms/{roomId}", room.getId())
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isForbidden());
    }
}
