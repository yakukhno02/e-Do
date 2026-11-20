package com.edo.dto.websocket;

import com.edo.dto.response.RoomDetailsResponse;

import java.util.UUID;

public class RoomUpdatedEvent {

    private UUID eventId;
    private String type = "ROOM_UPDATED";
    private RoomDetailsResponse room;

    public RoomUpdatedEvent() {
    }

    public RoomUpdatedEvent(UUID eventId, RoomDetailsResponse room) {
        this.eventId = eventId;
        this.type = "ROOM_UPDATED";
        this.room = room;
    }

    public static RoomUpdatedEvent of(RoomDetailsResponse room) {
        return new RoomUpdatedEvent(UUID.randomUUID(), room);
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RoomDetailsResponse getRoom() {
        return room;
    }

    public void setRoom(RoomDetailsResponse room) {
        this.room = room;
    }
}
