package com.edo.controller;

import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.TaskRequest;
import com.edo.dto.request.UpdateTaskStatusRequest;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;
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

    private String userOneToken;
    private String userTwoToken;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        userRepository.deleteAll();

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

    @Test
    @DisplayName("POST /tasks - successfully creates task and returns 201 Created")
    void createTask_Success() throws Exception {
        TaskRequest request = new TaskRequest(
                "Learn Spring Boot 3",
                "Study architecture and REST API",
                Instant.now().plusSeconds(86400),
                TaskStatus.TODO,
                TaskPriority.HIGH
        );

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title", is("Learn Spring Boot 3")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /tasks - validation failure on missing title returns 400 Bad Request")
    void createTask_MissingTitle_Returns400() throws Exception {
        TaskRequest request = new TaskRequest(
                "",
                "Description without title",
                null,
                TaskStatus.TODO,
                TaskPriority.LOW
        );

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /tasks - returns only tasks belonging to authenticated user")
    void getTasks_ReturnsOnlyUserTasks() throws Exception {
        TaskRequest task1 = new TaskRequest("User One Task", null, null, TaskStatus.TODO, TaskPriority.MEDIUM);
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task1)))
                .andExpect(status().isCreated());

        TaskRequest task2 = new TaskRequest("User Two Task", null, null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH);
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userTwoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("User One Task")));
    }

    @Test
    @DisplayName("GET /tasks - filters tasks by status and priority")
    void getTasks_FiltersCorrectly() throws Exception {
        TaskRequest taskTodo = new TaskRequest("Todo Task", null, null, TaskStatus.TODO, TaskPriority.LOW);
        TaskRequest taskDone = new TaskRequest("Done Task", null, null, TaskStatus.DONE, TaskPriority.HIGH);

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskTodo)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDone)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tasks?status=DONE")
                        .header("Authorization", "Bearer " + userOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Done Task")));

        mockMvc.perform(get("/api/v1/tasks?priority=LOW")
                        .header("Authorization", "Bearer " + userOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Todo Task")));
    }

    @Test
    @DisplayName("GET /tasks/{id} - accessing other user's task returns 404 Not Found (NFR-06 Data Isolation)")
    void getTaskById_OtherUserTask_Returns404() throws Exception {
        // User One створює задачу
        TaskRequest taskRequest = new TaskRequest("Private Task", null, null, TaskStatus.TODO, TaskPriority.HIGH);
        String createRes = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(createRes).get("id").asText();

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userTwoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("PATCH /tasks/{id}/status - updates task status")
    void updateTaskStatus_Success() throws Exception {
        TaskRequest taskRequest = new TaskRequest("Status Change Task", null, null, TaskStatus.TODO, TaskPriority.MEDIUM);
        String createRes = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(createRes).get("id").asText();

        UpdateTaskStatusRequest updateStatus = new UpdateTaskStatusRequest(TaskStatus.DONE);

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId)))
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    @DisplayName("DELETE /tasks/{id} - deletes user task and returns 204 No Content")
    void deleteTask_Success() throws Exception {
        TaskRequest taskRequest = new TaskRequest("To be deleted", null, null, TaskStatus.TODO, TaskPriority.LOW);
        String createRes = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(createRes).get("id").asText();

        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userOneToken))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.existsById(UUID.fromString(taskId)));
    }
}
