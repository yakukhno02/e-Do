package com.edo.repository;

import com.edo.entity.PomodoroSession;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, UUID> {
    Optional<PomodoroSession> findByIdAndUserId(UUID id, UUID userId);
    Optional<PomodoroSession> findByUserIdAndStatus(UUID userId, PomodoroSessionStatus status);
    List<PomodoroSession> findAllByUserIdOrderByStartTimeDesc(UUID userId);
    long countByUserIdAndStatus(UUID userId, PomodoroSessionStatus status);
    long countByUserIdAndTypeAndStatus(UUID userId, PomodoroSessionType type, PomodoroSessionStatus status);

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM PomodoroSession s WHERE s.user.id = :userId AND s.status = :status")
    long sumDurationMinutesByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") PomodoroSessionStatus status);

    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED' AND s.endTime >= :startTime AND s.endTime < :endTime")
    long countCompletedInPeriod(@Param("userId") UUID userId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM PomodoroSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED' AND s.endTime >= :startTime AND s.endTime < :endTime")
    long sumMinutesInPeriod(@Param("userId") UUID userId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
