package com.edo.controller;

import com.edo.dto.request.TaskRequest;
import com.edo.dto.request.UpdateTaskStatusRequest;
import com.edo.dto.response.TaskResponse;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;
import com.edo.security.UserPrincipal;
import com.edo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {
        List<TaskResponse> tasks = taskService.getTasks(currentUser.getId(), status, priority);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        TaskResponse response = taskService.getTaskById(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.updateTask(id, currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        TaskResponse response = taskService.updateTaskStatus(id, currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        taskService.deleteTask(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
