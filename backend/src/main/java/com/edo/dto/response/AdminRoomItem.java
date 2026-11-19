package com.edo.dto.response;

import com.edo.enums.RoomStatus;

import java.time.Instant;
import java.util.UUID;

public class AdminRoomItem {

    private UUID id;
    private String code;
    private String name;
    private UUID creatorId;
    private RoomStatus status;
    private int pomodoroDuration;
    private int breakDuration;
    private Instant createdAt;
    private int participantsCount;

    public AdminRoomItem() {
    }

    public AdminRoomItem(UUID id,
                         String code,
                         String name,
                         UUID creatorId,
                         RoomStatus status,
                         int pomodoroDuration,
                         int breakDuration,
                         Instant createdAt,
                         int participantsCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.creatorId = creatorId;
        this.status = status;
        this.pomodoroDuration = pomodoroDuration;
        this.breakDuration = breakDuration;
        this.createdAt = createdAt;
        this.participantsCount = participantsCount;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(int participantsCount) {
        this.participantsCount = participantsCount;
    }
}
