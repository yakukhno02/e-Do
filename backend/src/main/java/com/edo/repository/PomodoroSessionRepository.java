package com.edo.repository;

import com.edo.entity.PomodoroSession;
import com.edo.enums.PomodoroSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, UUID> {
    Optional<PomodoroSession> findByIdAndUserId(UUID id, UUID userId);
    Optional<PomodoroSession> findByUserIdAndStatus(UUID userId, PomodoroSessionStatus status);
    List<PomodoroSession> findAllByUserIdOrderByStartTimeDesc(UUID userId);
    long countByUserIdAndStatus(UUID userId, PomodoroSessionStatus status);
}
