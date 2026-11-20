package com.edo.websocket;

import com.edo.dto.request.LoginRequest;
import com.edo.entity.Room;
import com.edo.entity.RoomParticipant;
import com.edo.entity.User;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

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

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        stompClient = new WebSocketStompClient(new SockJsClient(transports));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
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

    private User createAndSaveUser(String email, String username, String name) {
        User user = new User(email, username, name, passwordEncoder.encode("Password123!"), UserRole.USER);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("STOMP connect succeeds with valid JWT Bearer header")
    void connectWithValidJwtSuccess() throws Exception {
        createAndSaveUser("ws_user@example.com", "ws_user", "WS User");
        String token = loginAndGetToken("ws_user@example.com", "Password123!");

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession session = stompClient.connectAsync(wsUrl, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    @DisplayName("STOMP connect fails when JWT header is missing")
    void connectWithoutJwtFails() {
        StompHeaders connectHeaders = new StompHeaders();
        String wsUrl = "ws://localhost:" + port + "/ws";

        assertThrows(ExecutionException.class, () ->
                stompClient.connectAsync(wsUrl, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                        .get(5, TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("STOMP participant can subscribe to room topic and receives ROOM_UPDATED event on action")
    void subscribeAndReceiveRoomUpdatedEvent() throws Exception {
        User creator = createAndSaveUser("creator@example.com", "creator", "Creator");
        String creatorToken = loginAndGetToken("creator@example.com", "Password123!");

        Room room = new Room("WS9999", "WebSocket Room", creator, 25, 5);
        room = roomRepository.save(room);
        roomParticipantRepository.save(new RoomParticipant(room, creator));

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + creatorToken);

        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession session = stompClient.connectAsync(wsUrl, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        CompletableFuture<JsonNode> messageFuture = new CompletableFuture<>();

        session.subscribe("/topic/rooms/" + room.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    JsonNode node = objectMapper.readTree((byte[]) payload);
                    messageFuture.complete(node);
                } catch (Exception e) {
                    messageFuture.completeExceptionally(e);
                }
            }
        });

        // Trigger REST action to start session, which emits ROOM_UPDATED event
        mockMvc.perform(post("/api/v1/rooms/{roomId}/start", room.getId())
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        JsonNode eventNode = messageFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(eventNode);
        assertNotNull(eventNode.get("eventId"));
        org.junit.jupiter.api.Assertions.assertEquals("ROOM_UPDATED", eventNode.get("type").asText());
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", eventNode.get("room").get("status").asText());

        session.disconnect();
    }
}
