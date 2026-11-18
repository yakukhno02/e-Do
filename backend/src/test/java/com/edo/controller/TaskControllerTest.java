package com.edo.controller;

import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.TaskRequest;
import com.edo.dto.request.UpdateTaskStatusRequest;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    private String userOneToken;
    private String userTwoToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        RegisterRequest userOne = new RegisterRequest(
                "User One", "user_one", "one@example.com", "Password123!", "Password123!"
        );
        String resOne = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userOne)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        userOneToken = objectMapper.readTree(resOne).get("token").asText();

        RegisterRequest userTwo = new RegisterRequest(
                "User Two", "user_two", "two@example.com", "Password123!", "Password123!"
        );
        String resTwo = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userTwo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        userTwoToken = objectMapper.readTree(resTwo).get("token").asText();
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
    @DisplayName("POST /tasks - successfully creates task and returns 201 Created")
    void createTask_Success() throws Exception {
        TaskRequest request = new TaskRequest(
                "Test Task",
                "Description for test task",
                Instant.now().plusSeconds(86400),
                TaskStatus.TODO,
                TaskPriority.HIGH
        );

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Test Task")))
                .andExpect(jsonPath("$.description", is("Description for test task")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /tasks - validation failure on missing title returns 400 Bad Request")
    void createTask_ValidationFailure() throws Exception {
        TaskRequest request = new TaskRequest(
                "",
                "Description",
                null,
                TaskStatus.TODO,
                TaskPriority.MEDIUM
        );

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("GET /tasks - returns only tasks belonging to authenticated user")
    void getTasks_UserIsolation() throws Exception {
        TaskRequest taskUserOne = new TaskRequest("Task One", "Desc", null, TaskStatus.TODO, TaskPriority.LOW);
        TaskRequest taskUserTwo = new TaskRequest("Task Two", "Desc", null, TaskStatus.TODO, TaskPriority.HIGH);

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskUserOne)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userTwoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskUserTwo)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task One")));

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userTwoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task Two")));
    }

    @Test
    @DisplayName("GET /tasks - filters tasks by status and priority")
    void getTasks_WithFilters() throws Exception {
        TaskRequest task1 = new TaskRequest("Task 1", null, null, TaskStatus.TODO, TaskPriority.HIGH);
        TaskRequest task2 = new TaskRequest("Task 2", null, null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH);
        TaskRequest task3 = new TaskRequest("Task 3", null, null, TaskStatus.TODO, TaskPriority.LOW);

        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + userOneToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + userOneToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task2))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + userOneToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task3))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tasks?status=TODO&priority=HIGH")
                        .header("Authorization", "Bearer " + userOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task 1")));
    }

    @Test
    @DisplayName("GET /tasks/{id} - accessing other user's task returns 404 Not Found (NFR-06 Data Isolation)")
    void getTaskById_DataIsolation() throws Exception {
        TaskRequest task = new TaskRequest("Secret Task", "Desc", null, TaskStatus.TODO, TaskPriority.HIGH);
        String res = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(res).get("id").asText();

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userTwoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("PATCH /tasks/{id}/status - updates task status")
    void patchTaskStatus_Success() throws Exception {
        TaskRequest task = new TaskRequest("Task", "Desc", null, TaskStatus.TODO, TaskPriority.MEDIUM);
        String res = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(res).get("id").asText();

        UpdateTaskStatusRequest updateStatus = new UpdateTaskStatusRequest(TaskStatus.DONE);
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    @DisplayName("DELETE /tasks/{id} - deletes user task and returns 204 No Content")
    void deleteTask_Success() throws Exception {
        TaskRequest task = new TaskRequest("Task to delete", null, null, TaskStatus.TODO, TaskPriority.LOW);
        String res = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(objectMapper.readTree(res).get("id").asText());

        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userOneToken))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.existsById(taskId));
    }
}
