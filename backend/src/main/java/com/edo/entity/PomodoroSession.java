package com.edo.entity;

import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pomodoro_sessions")
@EntityListeners(AuditingEntityListener.class)
public class PomodoroSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PomodoroSessionType type = PomodoroSessionType.INDIVIDUAL;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "break_duration_minutes", nullable = false)
    private int breakDurationMinutes;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PomodoroSessionStatus status = PomodoroSessionStatus.IN_PROGRESS;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PomodoroSession() {
    }

    public PomodoroSession(User user, Room room, PomodoroSessionType type, int durationMinutes, int breakDurationMinutes, Instant startTime) {
        this.user = user;
        this.room = room;
        this.type = type != null ? type : PomodoroSessionType.INDIVIDUAL;
        this.durationMinutes = durationMinutes;
        this.breakDurationMinutes = breakDurationMinutes;
        this.startTime = startTime;
        this.status = PomodoroSessionStatus.IN_PROGRESS;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public PomodoroSessionType getType() {
        return type;
    }

    public void setType(PomodoroSessionType type) {
        this.type = type;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getBreakDurationMinutes() {
        return breakDurationMinutes;
    }

    public void setBreakDurationMinutes(int breakDurationMinutes) {
        this.breakDurationMinutes = breakDurationMinutes;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public PomodoroSessionStatus getStatus() {
        return status;
    }

    public void setStatus(PomodoroSessionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
