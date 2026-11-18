package com.edo.controller;

import com.edo.dto.request.RoomCreateRequest;
import com.edo.dto.request.RoomJoinRequest;
import com.edo.dto.response.MessageResponse;
import com.edo.dto.response.RoomDetailsResponse;
import com.edo.security.UserPrincipal;
import com.edo.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomDetailsResponse> createRoom(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RoomCreateRequest request) {
        RoomDetailsResponse response = roomService.createRoom(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/join")
    public ResponseEntity<RoomDetailsResponse> joinRoom(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RoomJoinRequest request) {
        RoomDetailsResponse response = roomService.joinRoom(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDetailsResponse> getRoomDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        RoomDetailsResponse response = roomService.getRoomDetails(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<RoomDetailsResponse> startSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        RoomDetailsResponse response = roomService.startSession(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<RoomDetailsResponse> stopSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        RoomDetailsResponse response = roomService.stopSession(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<MessageResponse> leaveRoom(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        MessageResponse response = roomService.leaveRoom(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}
