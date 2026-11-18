package com.edo.service;

import com.edo.dto.request.RoomCreateRequest;
import com.edo.dto.request.RoomJoinRequest;
import com.edo.dto.response.MessageResponse;
import com.edo.dto.response.RoomDetailsResponse;

import java.util.UUID;

public interface RoomService {
    RoomDetailsResponse createRoom(UUID userId, RoomCreateRequest request);
    RoomDetailsResponse joinRoom(UUID userId, RoomJoinRequest request);
    RoomDetailsResponse getRoomDetails(UUID roomId, UUID userId);
    RoomDetailsResponse startSession(UUID roomId, UUID userId);
    RoomDetailsResponse stopSession(UUID roomId, UUID userId);
    MessageResponse leaveRoom(UUID roomId, UUID userId);
    void processExpiredRooms();
}
