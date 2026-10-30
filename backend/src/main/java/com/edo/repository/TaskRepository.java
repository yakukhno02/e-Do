package com.edo.repository;

import com.edo.entity.Task;
import com.edo.enums.TaskPriority;
import com.edo.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByIdAndUserId(UUID id, UUID userId);
    List<Task> findAllByUserId(UUID userId);
    List<Task> findAllByUserIdAndStatus(UUID userId, TaskStatus status);
    List<Task> findAllByUserIdAndPriority(UUID userId, TaskPriority priority);
    List<Task> findAllByUserIdAndStatusAndPriority(UUID userId, TaskStatus status, TaskPriority priority);
}
