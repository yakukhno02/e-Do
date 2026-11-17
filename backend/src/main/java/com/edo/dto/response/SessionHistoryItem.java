package com.edo.dto.response;

import com.edo.entity.PomodoroSession;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;

import java.time.Instant;
import java.util.UUID;

public class SessionHistoryItem {

    private UUID id;
    private UUID roomId;
    private PomodoroSessionType type;
    private int durationMinutes;
    private int breakDurationMinutes;
    private Instant startTime;
    private Instant endTime;
    private PomodoroSessionStatus status;

    public SessionHistoryItem() {
    }

    public SessionHistoryItem(UUID id, UUID roomId, PomodoroSessionType type, int durationMinutes,
                              int breakDurationMinutes, Instant startTime, Instant endTime, PomodoroSessionStatus status) {
        this.id = id;
        this.roomId = roomId;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.breakDurationMinutes = breakDurationMinutes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public static SessionHistoryItem fromEntity(PomodoroSession session) {
        return new SessionHistoryItem(
                session.getId(),
                session.getRoom() != null ? session.getRoom().getId() : null,
                session.getType(),
                session.getDurationMinutes(),
                session.getBreakDurationMinutes(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
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
}
