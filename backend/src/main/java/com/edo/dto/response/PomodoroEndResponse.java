package com.edo.dto.response;

import com.edo.enums.PomodoroSessionStatus;

import java.time.Instant;
import java.util.UUID;

public class PomodoroEndResponse {

    private UUID sessionId;
    private PomodoroSessionStatus status;
    private Instant endTime;
    private int earnedFocusMinutes;

    public PomodoroEndResponse() {
    }

    public PomodoroEndResponse(UUID sessionId, PomodoroSessionStatus status, Instant endTime, int earnedFocusMinutes) {
        this.sessionId = sessionId;
        this.status = status;
        this.endTime = endTime;
        this.earnedFocusMinutes = earnedFocusMinutes;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public PomodoroSessionStatus getStatus() {
        return status;
    }

    public void setStatus(PomodoroSessionStatus status) {
        this.status = status;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public int getEarnedFocusMinutes() {
        return earnedFocusMinutes;
    }

    public void setEarnedFocusMinutes(int earnedFocusMinutes) {
        this.earnedFocusMinutes = earnedFocusMinutes;
    }
}
