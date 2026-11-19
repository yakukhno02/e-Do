package com.edo.service;

import com.edo.dto.response.AdminRoomItem;
import com.edo.dto.response.AdminUserItem;
import com.edo.dto.response.PageResponse;

import java.util.UUID;

public interface AdminService {
    PageResponse<AdminUserItem> getUsers(int page, int size);
    void deleteUser(UUID userId);
    PageResponse<AdminRoomItem> getRooms(int page, int size);
    void deleteRoom(UUID roomId);
}
