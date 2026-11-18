package com.edo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class RoomParticipantResponse {

    private UUID userId;
    private String username;
    private String name;

    @JsonProperty("isCreator")
    private boolean isCreator;

    public RoomParticipantResponse() {
    }

    public RoomParticipantResponse(UUID userId, String username, String name, boolean isCreator) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.isCreator = isCreator;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCreator() {
        return isCreator;
    }

    public void setCreator(boolean creator) {
        isCreator = creator;
    }
}
