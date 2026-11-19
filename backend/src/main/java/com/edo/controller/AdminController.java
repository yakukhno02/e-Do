package com.edo.controller;

import com.edo.dto.response.AdminRoomItem;
import com.edo.dto.response.AdminUserItem;
import com.edo.dto.response.PageResponse;
import com.edo.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserItem>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AdminUserItem> response = adminService.getUsers(page, size);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms")
    public ResponseEntity<PageResponse<AdminRoomItem>> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AdminRoomItem> response = adminService.getRooms(page, size);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId) {
        adminService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}
