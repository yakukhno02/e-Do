package com.edo.service.impl;

import com.edo.dto.request.TaskRequest;
import com.edo.dto.request.UpdateTaskStatusRequest;
import com.edo.dto.response.TaskResponse;
import com.edo.entity.Task;
import com.edo.entity.User;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;
import com.edo.enums.UserRole;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.TaskRepository;
import com.edo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User testUser;
    private UUID userId;
    private UUID taskId;
    private Task testTask;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        testUser = new User("user@example.com", "username", "User Name", "passwordHash", UserRole.USER);
        testUser.setId(userId);

        testTask = new Task(testUser, "Test Title", "Test Description", Instant.now().plusSeconds(3600), TaskStatus.TODO, TaskPriority.MEDIUM);
        testTask.setId(taskId);
    }

    @Test
    @DisplayName("getTasks returns filtered tasks when status and priority are specified")
    void getTasksWithStatusAndPriority() {
        when(taskRepository.findAllByUserIdAndStatusAndPriority(userId, TaskStatus.TODO, TaskPriority.HIGH))
                .thenReturn(List.of(testTask));

        List<TaskResponse> responses = taskService.getTasks(userId, TaskStatus.TODO, TaskPriority.HIGH);

        assertEquals(1, responses.size());
        assertEquals("Test Title", responses.getFirst().getTitle());
        verify(taskRepository).findAllByUserIdAndStatusAndPriority(userId, TaskStatus.TODO, TaskPriority.HIGH);
    }

    @Test
    @DisplayName("getTasks returns all user tasks when filters are null")
    void getTasksWithoutFilters() {
        when(taskRepository.findAllByUserId(userId)).thenReturn(List.of(testTask));

        List<TaskResponse> responses = taskService.getTasks(userId, null, null);

        assertEquals(1, responses.size());
        verify(taskRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("getTaskById returns task response when task exists for user")
    void getTaskByIdSuccess() {
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(testTask));

        TaskResponse response = taskService.getTaskById(taskId, userId);

        assertNotNull(response);
        assertEquals(taskId, response.getId());
        assertEquals("Test Title", response.getTitle());
    }

    @Test
    @DisplayName("getTaskById throws ResourceNotFoundException when task does not belong to user")
    void getTaskByIdNotFound() {
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(taskId, userId));
    }

    @Test
    @DisplayName("createTask saves task and returns response when user exists")
    void createTaskSuccess() {
        TaskRequest request = new TaskRequest();
        request.setTitle("New Task");
        request.setDescription("New Desc");
        request.setStatus(TaskStatus.TODO);
        request.setPriority(TaskPriority.HIGH);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            return task;
        });

        TaskResponse response = taskService.createTask(userId, request);

        assertNotNull(response);
        assertEquals("New Task", response.getTitle());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask throws ResourceNotFoundException when user is not found")
    void createTaskUserNotFound() {
        TaskRequest request = new TaskRequest();
        request.setTitle("New Task");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.createTask(userId, request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("updateTask updates task fields and returns updated response")
    void updateTaskSuccess() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Desc");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.LOW);

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask(taskId, userId, request);

        assertEquals("Updated Title", response.getTitle());
        assertEquals("Updated Desc", response.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(TaskPriority.LOW, response.getPriority());
    }

    @Test
    @DisplayName("updateTaskStatus updates status only")
    void updateTaskStatusSuccess() {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.DONE);

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(taskId, userId, request);

        assertEquals(TaskStatus.DONE, response.getStatus());
    }

    @Test
    @DisplayName("deleteTask removes task when found for user")
    void deleteTaskSuccess() {
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(testTask));

        taskService.deleteTask(taskId, userId);

        verify(taskRepository).delete(testTask);
    }

    @Test
    @DisplayName("deleteTask throws ResourceNotFoundException when task not found")
    void deleteTaskNotFound() {
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(taskId, userId));
        verify(taskRepository, never()).delete(any());
    }
}
