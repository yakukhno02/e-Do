package com.edo.service;

import com.edo.dto.request.TaskRequest;
import com.edo.dto.request.UpdateTaskStatusRequest;
import com.edo.dto.response.TaskResponse;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    List<TaskResponse> getTasks(UUID userId, TaskStatus status, TaskPriority priority);
    TaskResponse getTaskById(UUID taskId, UUID userId);
    TaskResponse createTask(UUID userId, TaskRequest request);
    TaskResponse updateTask(UUID taskId, UUID userId, TaskRequest request);
    TaskResponse updateTaskStatus(UUID taskId, UUID userId, UpdateTaskStatusRequest request);
    void deleteTask(UUID taskId, UUID userId);
}
