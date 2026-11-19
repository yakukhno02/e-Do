package com.edo.service.impl;

import com.edo.dto.response.AdminRoomItem;
import com.edo.dto.response.AdminUserItem;
import com.edo.dto.response.PageResponse;
import com.edo.entity.Room;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.enums.RoomStatus;
import com.edo.enums.UserRole;
import com.edo.exception.BadRequestException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.RoomParticipantRepository;
import com.edo.repository.RoomRepository;
import com.edo.repository.TaskRepository;
import com.edo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PomodoroSessionRepository sessionRepository;

    @Mock
    private ProductivityGoalRepository goalRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;
    private UUID userId;
    private Room testRoom;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        testUser = new User("user@example.com", "username", "Name", "hash", UserRole.USER);
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());

        testRoom = new Room("CODE12", "Study Room", testUser, 25, 5);
        testRoom.setId(roomId);
        testRoom.setStatus(RoomStatus.WAITING);
        testRoom.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("getUsers returns paginated admin user items with statistics")
    void getUsersSuccess() {
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(sessionRepository.countByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED)).thenReturn(5L);
        when(sessionRepository.sumDurationMinutesByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED)).thenReturn(125L);
        when(sessionRepository.countByUserIdAndTypeAndStatus(userId, PomodoroSessionType.SHARED, PomodoroSessionStatus.COMPLETED)).thenReturn(2L);

        PageResponse<AdminUserItem> result = adminService.getUsers(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        AdminUserItem item = result.getContent().getFirst();
        assertEquals(userId, item.getId());
        assertEquals(5L, item.getCompletedPomodoros());
        assertEquals(125L, item.getTotalFocusMinutes());
        assertEquals(2L, item.getCompletedSharedSessions());
    }

    @Test
    @DisplayName("getUsers throws BadRequestException on invalid pagination")
    void getUsersInvalidPagination() {
        assertThrows(BadRequestException.class, () -> adminService.getUsers(-1, 20));
        assertThrows(BadRequestException.class, () -> adminService.getUsers(0, 0));
        assertThrows(BadRequestException.class, () -> adminService.getUsers(0, 101));
    }

    @Test
    @DisplayName("deleteUser performs complete cascade deletion of user and related entities")
    void deleteUserSuccess() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roomRepository.findAllByCreatorId(userId)).thenReturn(List.of(testRoom));

        adminService.deleteUser(userId);

        verify(sessionRepository).setRoomNullByRoomId(roomId);
        verify(roomRepository).delete(testRoom);
        verify(roomParticipantRepository).deleteAllByUserId(userId);
        verify(sessionRepository).deleteAllByUserId(userId);
        verify(goalRepository).deleteAllByUserId(userId);
        verify(taskRepository).deleteAllByUserId(userId);
        verify(passwordResetTokenRepository).deleteAllByUserId(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteUser throws ResourceNotFoundException when user does not exist")
    void deleteUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.deleteUser(userId));
    }

    @Test
    @DisplayName("getRooms returns paginated rooms with participant count")
    void getRoomsSuccess() {
        Page<Room> roomPage = new PageImpl<>(List.of(testRoom));
        when(roomRepository.findAll(any(Pageable.class))).thenReturn(roomPage);
        when(roomParticipantRepository.countByRoomId(roomId)).thenReturn(3);

        PageResponse<AdminRoomItem> result = adminService.getRooms(0, 20);

        assertEquals(1, result.getContent().size());
        AdminRoomItem item = result.getContent().getFirst();
        assertEquals(roomId, item.getId());
        assertEquals(3, item.getParticipantsCount());
    }

    @Test
    @DisplayName("deleteRoom clears session room references and deletes room")
    void deleteRoomSuccess() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));

        adminService.deleteRoom(roomId);

        verify(sessionRepository).setRoomNullByRoomId(roomId);
        verify(roomRepository).delete(testRoom);
    }

    @Test
    @DisplayName("deleteRoom throws ResourceNotFoundException when room does not exist")
    void deleteRoomNotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.deleteRoom(roomId));
    }
}
