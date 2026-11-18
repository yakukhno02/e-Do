package com.edo.dto.response;

import com.edo.enums.RoomStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RoomDetailsResponse {

    private UUID id;
    private String code;
    private String name;
    private UUID creatorId;
    private RoomStatus status;
    private int pomodoroDuration;
    private int breakDuration;
    private Instant currentSessionStart;
    private Instant serverTime;
    private long version;
    private List<RoomParticipantResponse> participants;

    public RoomDetailsResponse() {
    }

    public RoomDetailsResponse(UUID id, String code, String name, UUID creatorId, RoomStatus status,
                               int pomodoroDuration, int breakDuration, Instant currentSessionStart,
                               Instant serverTime, long version, List<RoomParticipantResponse> participants) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.creatorId = creatorId;
        this.status = status;
        this.pomodoroDuration = pomodoroDuration;
        this.breakDuration = breakDuration;
        this.currentSessionStart = currentSessionStart;
        this.serverTime = serverTime;
        this.version = version;
        this.participants = participants;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public int getPomodoroDuration() {
        return pomodoroDuration;
    }

    public void setPomodoroDuration(int pomodoroDuration) {
        this.pomodoroDuration = pomodoroDuration;
    }

    public int getBreakDuration() {
        return breakDuration;
    }

    public void setBreakDuration(int breakDuration) {
        this.breakDuration = breakDuration;
    }

    public Instant getCurrentSessionStart() {
        return currentSessionStart;
    }

    public void setCurrentSessionStart(Instant currentSessionStart) {
        this.currentSessionStart = currentSessionStart;
    }

    public Instant getServerTime() {
        return serverTime;
    }

    public void setServerTime(Instant serverTime) {
        this.serverTime = serverTime;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<RoomParticipantResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(List<RoomParticipantResponse> participants) {
        this.participants = participants;
    }
}
