package com.edo.controller;

import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.RoomCreateRequest;
import com.edo.dto.request.RoomJoinRequest;
import com.edo.entity.PomodoroSession;
import com.edo.entity.Room;
import com.edo.entity.RoomParticipant;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private PomodoroSessionRepository sessionRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private ProductivityGoalRepository goalRepository;

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
    @DisplayName("POST /api/v1/rooms - successfully creates a room with creator as participant")
    void createRoom_Success() throws Exception {
        RoomCreateRequest request = new RoomCreateRequest("Study Group", 25, 5);

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Study Group")))
                .andExpect(jsonPath("$.pomodoroDuration", is(25)))
                .andExpect(jsonPath("$.breakDuration", is(5)))
                .andExpect(jsonPath("$.status", is("WAITING")))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(jsonPath("$.creatorId", is(user1.getId().toString())))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].userId", is(user1.getId().toString())))
                .andExpect(jsonPath("$.participants[0].isCreator", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/rooms - returns 400 Bad Request on invalid input")
    void createRoom_InvalidInput() throws Exception {
        RoomCreateRequest request = new RoomCreateRequest("", 0, 70);

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/rooms - returns 401 Unauthorized without token")
    void createRoom_Unauthorized() throws Exception {
        RoomCreateRequest request = new RoomCreateRequest("Study Group", 25, 5);

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/rooms/join - second user joins room successfully")
    void joinRoom_Success() throws Exception {
        RoomCreateRequest createRequest = new RoomCreateRequest("Study Group", 25, 5);
        String createRes = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(createRes).get("code").asText();

        RoomJoinRequest joinRequest = new RoomJoinRequest(code.toLowerCase());

        mockMvc.perform(post("/api/v1/rooms/join")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    @DisplayName("POST /api/v1/rooms/join - idempotent join returns room details without duplicate participant")
    void joinRoom_Idempotent() throws Exception {
        RoomCreateRequest createRequest = new RoomCreateRequest("Study Group", 25, 5);
        String createRes = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(createRes).get("code").asText();

        RoomJoinRequest joinRequest = new RoomJoinRequest(code);

        mockMvc.perform(post("/api/v1/rooms/join")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/rooms/join - returns 404 when room code not found")
    void joinRoom_NotFound() throws Exception {
        RoomJoinRequest joinRequest = new RoomJoinRequest("ZZZZZZ");

        mockMvc.perform(post("/api/v1/rooms/join")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/v1/rooms/{id} - returns room details for participant")
    void getRoomDetails_Success() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        roomParticipantRepository.save(new RoomParticipant(room, user1));

        mockMvc.perform(get("/api/v1/rooms/" + room.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(room.getId().toString())))
                .andExpect(jsonPath("$.name", is("Group Room")));
    }

    @Test
    @DisplayName("GET /api/v1/rooms/{id} - returns 404 Not Found for non-participant")
    void getRoomDetails_NonParticipant_Returns404() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        roomParticipantRepository.save(new RoomParticipant(room, user1));

        mockMvc.perform(get("/api/v1/rooms/" + room.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("POST /api/v1/rooms/{id}/start - starts session and creates PomodoroSession for all participants")
    void startSession_Success() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        roomParticipantRepository.save(new RoomParticipant(room, user1));
        roomParticipantRepository.save(new RoomParticipant(room, user2));

        mockMvc.perform(post("/api/v1/rooms/" + room.getId() + "/start")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.currentSessionStart", notNullValue()));

        assertEquals(2, sessionRepository.findAllByRoomIdAndStatus(room.getId(), PomodoroSessionStatus.IN_PROGRESS).size());
    }

    @Test
    @DisplayName("POST /api/v1/rooms/{id}/start - returns 409 Conflict when a participant already has an active session")
    void startSession_ConflictWhenParticipantHasActiveSession() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        roomParticipantRepository.save(new RoomParticipant(room, user1));
        roomParticipantRepository.save(new RoomParticipant(room, user2));

        // Create an individual active session for user2
        PomodoroSession individualSession = new PomodoroSession(
                user2, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now()
        );
        sessionRepository.save(individualSession);

        mockMvc.perform(post("/api/v1/rooms/" + room.getId() + "/start")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("One or more participants already have an active Pomodoro session")));
    }

    @Test
    @DisplayName("POST /api/v1/rooms/{id}/stop - stops active session and cancels sessions without awarding minutes")
    void stopSession_Success() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        roomParticipantRepository.save(new RoomParticipant(room, user1));

        mockMvc.perform(post("/api/v1/rooms/" + room.getId() + "/start")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/rooms/" + room.getId() + "/stop")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FINISHED")))
                .andExpect(jsonPath("$.currentSessionStart").doesNotExist());

        PomodoroSession session = sessionRepository.findAll().getFirst();
        assertEquals(PomodoroSessionStatus.CANCELLED, session.getStatus());
    }

    @Test
    @DisplayName("POST /api/v1/rooms/{id}/stop - returns 409 Conflict if room is not active")
    void stopSession_ConflictIfNotActive() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        roomParticipantRepository.save(new RoomParticipant(room, user1));

        mockMvc.perform(post("/api/v1/rooms/" + room.getId() + "/stop")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("POST /api/v1/rooms/{id}/leave - transfers creator when creator leaves")
    void leaveRoom_TransferCreator() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        RoomParticipant p1 = roomParticipantRepository.save(new RoomParticipant(room, user1));
        RoomParticipant p2 = roomParticipantRepository.save(new RoomParticipant(room, user2));

        mockMvc.perform(post("/api/v1/rooms/" + room.getId() + "/leave")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Left the room successfully")));

        Room updatedRoom = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(user2.getId(), updatedRoom.getCreator().getId());
        assertFalse(roomParticipantRepository.existsByRoomIdAndUserId(room.getId(), user1.getId()));
        assertTrue(roomParticipantRepository.existsByRoomIdAndUserId(room.getId(), user2.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/rooms/{id}/leave - deletes empty room when last participant leaves")
    void leaveRoom_DeletesEmptyRoom() throws Exception {
        Room room = roomRepository.save(new Room("ABC234", "Group Room", user1, 25, 5));
        roomParticipantRepository.save(new RoomParticipant(room, user1));

        mockMvc.perform(post("/api/v1/rooms/" + room.getId() + "/leave")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Left the room successfully")));

        assertFalse(roomRepository.existsById(room.getId()));
    }
}
