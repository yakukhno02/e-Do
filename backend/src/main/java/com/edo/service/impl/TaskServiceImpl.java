package com.edo.service.impl;

import com.edo.dto.request.TaskRequest;
import com.edo.dto.request.UpdateTaskStatusRequest;
import com.edo.dto.response.TaskResponse;
import com.edo.entity.Task;
import com.edo.entity.User;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.TaskRepository;
import com.edo.repository.UserRepository;
import com.edo.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(UUID userId, TaskStatus status, TaskPriority priority) {
        List<Task> tasks;
        if (status != null && priority != null) {
            tasks = taskRepository.findAllByUserIdAndStatusAndPriority(userId, status, priority);
        } else if (status != null) {
            tasks = taskRepository.findAllByUserIdAndStatus(userId, status);
        } else if (priority != null) {
            tasks = taskRepository.findAllByUserIdAndPriority(userId, priority);
        } else {
            tasks = taskRepository.findAllByUserId(userId);
        }

        return tasks.stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId, UUID userId) {
        // NFR-06: Якщо задача належить іншому юзеру або не існує -> 404 Not Found
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional
    public TaskResponse createTask(UUID userId, TaskRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Task task = new Task(
                user,
                request.getTitle(),
                request.getDescription(),
                request.getDeadline(),
                request.getStatus(),
                request.getPriority()
        );

        Task savedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID taskId, UUID userId, TaskRequest request) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());

        Task updatedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(updatedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(UUID taskId, UUID userId, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        task.setStatus(request.getStatus());

        Task updatedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(UUID taskId, UUID userId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        taskRepository.delete(task);
    }
}
