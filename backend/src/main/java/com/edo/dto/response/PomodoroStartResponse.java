package com.edo.dto.response;

import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;

import java.time.Instant;
import java.util.UUID;

public class PomodoroStartResponse {

    private UUID sessionId;
    private PomodoroSessionType type;
    private int durationMinutes;
    private int breakDurationMinutes;
    private Instant startTime;
    private PomodoroSessionStatus status;

    public PomodoroStartResponse() {
    }

    public PomodoroStartResponse(UUID sessionId, PomodoroSessionType type, int durationMinutes,
                                 int breakDurationMinutes, Instant startTime, PomodoroSessionStatus status) {
        this.sessionId = sessionId;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.breakDurationMinutes = breakDurationMinutes;
        this.startTime = startTime;
        this.status = status;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
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

    public PomodoroSessionStatus getStatus() {
        return status;
    }

    public void setStatus(PomodoroSessionStatus status) {
        this.status = status;
    }
}
